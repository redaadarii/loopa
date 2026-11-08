package app.loopa.integration.internal;

import app.loopa.catalog.OrderUpsert;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Convertit les commandes Shopify (webhook REST ou GraphQL) en {@link OrderUpsert}. */
final class ShopifyOrderMapper {

    private ShopifyOrderMapper() {
    }

    /** Charge utile REST des webhooks orders/create et orders/updated. */
    static OrderUpsert fromWebhook(JsonNode o) {
        String currency = o.path("currency").asText("MAD");
        List<OrderUpsert.Line> lines = new ArrayList<>();
        for (JsonNode li : o.path("line_items")) {
            lines.add(new OrderUpsert.Line(
                    li.path("id").asText(),
                    text(li, "product_id"),
                    text(li, "variant_id"),
                    li.path("title").asText(),
                    text(li, "variant_title"),
                    text(li, "sku"),
                    null,
                    li.path("quantity").asInt(1),
                    toMinor(li.path("price").asText("0")),
                    null));
        }
        JsonNode customer = o.path("customer");
        String name = (customer.path("first_name").asText("") + " " + customer.path("last_name").asText("")).trim();
        Instant fulfilledAt = null;
        for (JsonNode f : o.path("fulfillments")) {
            Instant at = Instant.parse(f.path("created_at").asText());
            fulfilledAt = fulfilledAt == null || at.isAfter(fulfilledAt) ? at : fulfilledAt;
        }
        String gateway = o.path("payment_gateway_names").isArray() && !o.path("payment_gateway_names").isEmpty()
                ? o.path("payment_gateway_names").get(0).asText() : null;
        return new OrderUpsert(
                o.path("id").asText(),
                o.path("name").asText(),
                name.isEmpty() ? null : name,
                text(o, "email"),
                firstNonBlank(text(o, "phone"), text(customer, "phone"), text(o.path("shipping_address"), "phone")),
                currency,
                toMinor(o.path("total_price").asText("0")),
                gateway,
                Instant.parse(o.path("created_at").asText()),
                fulfilledAt,
                lines);
    }

    /** Nœud {@code Order} de l'API GraphQL (voir la requête de synchronisation). */
    static OrderUpsert fromGraphql(JsonNode o) {
        List<OrderUpsert.Line> lines = new ArrayList<>();
        for (JsonNode edge : o.path("lineItems").path("edges")) {
            JsonNode li = edge.path("node");
            lines.add(new OrderUpsert.Line(
                    numericId(li.path("id").asText()),
                    numericId(li.path("product").path("id").asText(null)),
                    numericId(li.path("variant").path("id").asText(null)),
                    li.path("title").asText(),
                    text(li, "variantTitle"),
                    text(li, "sku"),
                    text(li.path("product"), "productType"),
                    li.path("quantity").asInt(1),
                    toMinor(li.path("originalUnitPriceSet").path("shopMoney").path("amount").asText("0")),
                    text(li.path("image"), "url")));
        }
        JsonNode customer = o.path("customer");
        JsonNode total = o.path("totalPriceSet").path("shopMoney");
        String fulfilledAt = null;
        for (JsonNode f : o.path("fulfillments")) {
            String at = f.path("createdAt").asText(null);
            if (at != null && (fulfilledAt == null || at.compareTo(fulfilledAt) > 0)) {
                fulfilledAt = at;
            }
        }
        return new OrderUpsert(
                numericId(o.path("id").asText()),
                o.path("name").asText(),
                text(customer, "displayName"),
                text(o, "email"),
                firstNonBlank(text(o, "phone"), text(customer, "phone")),
                total.path("currencyCode").asText("MAD"),
                toMinor(total.path("amount").asText("0")),
                o.path("paymentGatewayNames").isArray() && !o.path("paymentGatewayNames").isEmpty()
                        ? o.path("paymentGatewayNames").get(0).asText() : null,
                Instant.parse(o.path("createdAt").asText()),
                fulfilledAt == null ? null : Instant.parse(fulfilledAt),
                lines);
    }

    /** « 299.00 » → 29900. */
    static long toMinor(String amount) {
        return new BigDecimal(amount).movePointRight(2).longValueExact();
    }

    /** « gid://shopify/LineItem/123 » → « 123 ». */
    static String numericId(String gid) {
        if (gid == null) {
            return null;
        }
        int slash = gid.lastIndexOf('/');
        return slash >= 0 ? gid.substring(slash + 1) : gid;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
