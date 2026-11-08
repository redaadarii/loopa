package app.loopa.integration.internal;

import app.loopa.catalog.CatalogApi;
import app.loopa.catalog.OrderView;
import app.loopa.integration.IntegrationApi;
import app.loopa.returns.ReturnView;
import app.loopa.shared.BusinessException;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Logique Shopify : installation, abonnements webhooks, synchronisation des commandes et exécution
 * des résolutions (carte cadeau, remboursement, commande d'échange).
 */
@Service
class ShopifyService implements IntegrationApi {

    private static final Logger log = LoggerFactory.getLogger(ShopifyService.class);

    static final List<String> WEBHOOK_TOPICS = List.of("ORDERS_CREATE", "ORDERS_UPDATED", "APP_UNINSTALLED");

    private static final String SHOP_QUERY = "{ shop { name email currencyCode } }";

    private static final String ORDERS_QUERY = """
            query Orders($cursor: String, $query: String) {
              orders(first: 50, after: $cursor, query: $query, sortKey: CREATED_AT, reverse: true) {
                pageInfo { hasNextPage endCursor }
                edges { node {
                  id name email phone createdAt paymentGatewayNames
                  customer { displayName phone }
                  totalPriceSet { shopMoney { amount currencyCode } }
                  fulfillments { createdAt }
                  lineItems(first: 50) { edges { node {
                    id title variantTitle sku quantity
                    originalUnitPriceSet { shopMoney { amount } }
                    product { id productType } variant { id } image { url }
                  } } }
                } }
              }
            }""";

    private static final String WEBHOOK_MUTATION = """
            mutation Subscribe($topic: WebhookSubscriptionTopic!, $url: URL!) {
              webhookSubscriptionCreate(topic: $topic, webhookSubscription: { callbackUrl: $url, format: JSON }) {
                userErrors { field message }
              }
            }""";

    private static final String GIFT_CARD_MUTATION = """
            mutation GiftCard($input: GiftCardCreateInput!) {
              giftCardCreate(input: $input) { giftCard { id } userErrors { field message } }
            }""";

    private static final String REFUND_MUTATION = """
            mutation Refund($input: RefundInput!) {
              refundCreate(input: $input) { refund { id } userErrors { field message } }
            }""";

    private static final String DRAFT_ORDER_MUTATION = """
            mutation Exchange($input: DraftOrderInput!) {
              draftOrderCreate(input: $input) { draftOrder { id name } userErrors { field message } }
            }""";

    private final ShopifyClient client;
    private final ShopifyProperties properties;
    private final TenancyApi tenancy;
    private final CatalogApi catalog;
    private final Clock clock;
    private final Map<UUID, Instant> lastSync = new ConcurrentHashMap<>();

    ShopifyService(ShopifyClient client, ShopifyProperties properties, TenancyApi tenancy, CatalogApi catalog,
                   Clock clock) {
        this.client = client;
        this.properties = properties;
        this.tenancy = tenancy;
        this.catalog = catalog;
        this.clock = clock;
    }

    /** Fin de l'OAuth : enregistre la boutique, abonne les webhooks et importe les commandes récentes. */
    ShopView completeInstall(String shopDomain, String code) {
        String token = client.exchangeCode(shopDomain, code);
        JsonNode shopInfo = client.graphql(shopDomain, token, SHOP_QUERY, Map.of()).path("shop");
        ShopView shop = tenancy.registerShopifyShop(shopDomain, shopInfo.path("name").asText(shopDomain),
                shopInfo.path("currencyCode").asText("MAD"), shopInfo.path("email").asText(null), token);
        for (String topic : WEBHOOK_TOPICS) {
            client.graphql(shopDomain, token, WEBHOOK_MUTATION,
                    Map.of("topic", topic, "url", properties.apiUrl() + "/webhooks/shopify"));
        }
        try {
            syncOrders(shop.id(), Duration.ofDays(60));
        } catch (RuntimeException e) {
            log.warn("Import initial des commandes de {} en échec : {}", shopDomain, e.getMessage());
        }
        return shop;
    }

    /** Importe les commandes créées sur la période ; renvoie le nombre de commandes traitées. */
    int syncOrders(UUID shopId, Duration period) {
        ShopView shop = tenancy.findById(shopId).orElseThrow();
        String token = tenancy.shopifyAccessToken(shopId).orElseThrow(() ->
                new BusinessException("SHOPIFY_NOT_CONNECTED", HttpStatus.CONFLICT, "Boutique Shopify non connectée."));
        String since = clock.instant().minus(period).toString();
        int count = 0;
        String cursor = null;
        do {
            Map<String, Object> variables = new HashMap<>();
            variables.put("cursor", cursor);
            variables.put("query", "created_at:>=" + since);
            JsonNode orders = client.graphql(shop.shopDomain(), token, ORDERS_QUERY, variables).path("orders");
            for (JsonNode edge : orders.path("edges")) {
                var upsert = ShopifyOrderMapper.fromGraphql(edge.path("node"));
                TenantContext.runAs(shopId, () -> catalog.upsert(upsert));
                count++;
            }
            cursor = orders.path("pageInfo").path("hasNextPage").asBoolean() ?
                    orders.path("pageInfo").path("endCursor").asText() : null;
        } while (cursor != null && count < 2_000);
        lastSync.put(shopId, clock.instant());
        return count;
    }

    Instant lastSyncAt(UUID shopId) {
        return lastSync.get(shopId);
    }

    @Override
    public String executeShopifyResolution(UUID shopId, ReturnView r) {
        ShopView shop = tenancy.findById(shopId).orElseThrow();
        String token = tenancy.shopifyAccessToken(shopId)
                .orElseThrow(() -> new IllegalStateException("Boutique Shopify non connectée"));
        OrderView order = catalog.findById(r.orderId())
                .orElseThrow(() -> new IllegalStateException("Commande introuvable : " + r.orderName()));
        return switch (r.resolutionType()) {
            case "STORE_CREDIT" -> giftCard(shop, token, r);
            case "REFUND" -> refund(shop, token, r, order);
            case "EXCHANGE" -> exchange(shop, token, r, order);
            default -> throw new IllegalStateException("Solution inconnue : " + r.resolutionType());
        };
    }

    private String giftCard(ShopView shop, String token, ReturnView r) {
        Map<String, Object> input = new HashMap<>();
        input.put("initialValue", decimal(r.creditMinor()));
        input.put("note", "Avoir Loopa " + r.number());
        JsonNode data = client.graphql(shop.shopDomain(), token, GIFT_CARD_MUTATION, Map.of("input", input));
        return data.path("giftCardCreate").path("giftCard").path("id").asText();
    }

    private String refund(ShopView shop, String token, ReturnView r, OrderView order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ReturnView.Line l : r.lines()) {
            items.add(Map.of("lineItemId", "gid://shopify/LineItem/" + l.orderLineId(), "quantity", l.quantity(),
                    "restockType", "RETURN"));
        }
        Map<String, Object> input = new HashMap<>();
        input.put("orderId", "gid://shopify/Order/" + order.externalId());
        input.put("note", "Retour Loopa " + r.number());
        input.put("notify", true);
        input.put("refundLineItems", items);
        // Paiement à la livraison : pas de transaction à rembourser, le virement est fait par le marchand.
        JsonNode data = client.graphql(shop.shopDomain(), token, REFUND_MUTATION, Map.of("input", input));
        return data.path("refundCreate").path("refund").path("id").asText();
    }

    private String exchange(ShopView shop, String token, ReturnView r, OrderView order) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ReturnView.Line l : r.lines()) {
            OrderView.Line original = order.line(l.orderLineId()).orElseThrow();
            items.add(Map.of("variantId", "gid://shopify/ProductVariant/" + original.variantId(),
                    "quantity", l.quantity(),
                    "appliedDiscount", Map.of("valueType", "PERCENTAGE", "value", 100.0,
                            "title", "Échange " + r.number())));
        }
        String preferences = r.lines().stream().filter(l -> l.exchangeVariantId() != null)
                .map(l -> l.title() + " → " + l.exchangeVariantId()).reduce((a, b) -> a + "; " + b).orElse("");
        Map<String, Object> input = new HashMap<>();
        input.put("email", r.customerEmail());
        input.put("note", "Échange Loopa " + r.number() + (preferences.isEmpty() ? "" : " — " + preferences));
        input.put("lineItems", items);
        input.put("tags", List.of("loopa-exchange"));
        JsonNode data = client.graphql(shop.shopDomain(), token, DRAFT_ORDER_MUTATION, Map.of("input", input));
        return data.path("draftOrderCreate").path("draftOrder").path("id").asText();
    }

    private static String decimal(long minor) {
        return BigDecimal.valueOf(minor).movePointLeft(2).toPlainString();
    }
}
