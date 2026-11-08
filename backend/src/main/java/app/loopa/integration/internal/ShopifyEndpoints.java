package app.loopa.integration.internal;

import app.loopa.catalog.CatalogApi;
import app.loopa.security.Principals;
import app.loopa.shared.BusinessException;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Points d'entrée Shopify :
 * <ul>
 *   <li>{@code GET /auth/shopify/install} puis {@code /callback} : OAuth d'installation ;</li>
 *   <li>{@code POST /webhooks/shopify} : webhooks signés (commandes, désinstallation, RGPD) ;</li>
 *   <li>{@code /api/admin/v1/integrations} : état de la connexion et synchronisation manuelle.</li>
 * </ul>
 */
@RestController
@Tag(name = "Intégration Shopify")
class ShopifyEndpoints {

    private static final Logger log = LoggerFactory.getLogger(ShopifyEndpoints.class);
    private static final int MAX_SEEN_WEBHOOKS = 10_000;

    private final ShopifyService shopify;
    private final ShopifyProperties properties;
    private final TenancyApi tenancy;
    private final CatalogApi catalog;
    private final ObjectMapper json;
    private final Clock clock;
    /** Anti-rejeu : Shopify peut livrer plusieurs fois le même webhook (X-Shopify-Webhook-Id). */
    private final Set<String> seenWebhooks = Collections.synchronizedSet(Collections.newSetFromMap(
            new LinkedHashMap<String, Boolean>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_SEEN_WEBHOOKS;
                }
            }));

    ShopifyEndpoints(ShopifyService shopify, ShopifyProperties properties, TenancyApi tenancy, CatalogApi catalog,
                     ObjectMapper json, Clock clock) {
        this.shopify = shopify;
        this.properties = properties;
        this.tenancy = tenancy;
        this.catalog = catalog;
        this.json = json;
        this.clock = clock;
    }

    /* ---------- OAuth ---------- */

    @Hidden
    @GetMapping("/auth/shopify/install")
    ResponseEntity<Void> install(@RequestParam Map<String, String> params) {
        requireConfigured();
        String shop = params.get("shop");
        if (!ShopifySignatures.isValidShopDomain(shop)) {
            throw new BusinessException("INVALID_SHOP", HttpStatus.BAD_REQUEST, "Domaine de boutique invalide.");
        }
        if (params.containsKey("hmac") && !ShopifySignatures.verifyQuery(params, properties.apiSecret())) {
            throw new BusinessException("INVALID_HMAC", HttpStatus.UNAUTHORIZED, "Signature Shopify invalide.");
        }
        String state = ShopifySignatures.signState(shop, clock.instant().plus(Duration.ofMinutes(10)),
                properties.apiSecret());
        String url = "https://" + shop + "/admin/oauth/authorize"
                + "?client_id=" + enc(properties.apiKey())
                + "&scope=" + enc(properties.scopes())
                + "&redirect_uri=" + enc(properties.apiUrl() + "/auth/shopify/callback")
                + "&state=" + enc(state);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @Hidden
    @GetMapping("/auth/shopify/callback")
    ResponseEntity<Void> callback(@RequestParam Map<String, String> params) {
        requireConfigured();
        String shop = params.get("shop");
        if (!ShopifySignatures.isValidShopDomain(shop)
                || !ShopifySignatures.verifyQuery(params, properties.apiSecret())
                || !ShopifySignatures.verifyState(params.get("state"), shop, clock.instant(), properties.apiSecret())) {
            throw new BusinessException("INVALID_OAUTH", HttpStatus.UNAUTHORIZED, "Requête OAuth Shopify invalide.");
        }
        ShopView installed = shopify.completeInstall(shop, params.get("code"));
        log.info("Application installée sur {}", shop);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(properties.adminUrl() + "/login?installed=" + enc(installed.handle())))
                .build();
    }

    /* ---------- Webhooks ---------- */

    @Hidden
    @PostMapping("/webhooks/shopify")
    ResponseEntity<Void> webhook(@RequestBody byte[] body,
                                 @RequestHeader("X-Shopify-Topic") String topic,
                                 @RequestHeader("X-Shopify-Shop-Domain") String shopDomain,
                                 @RequestHeader("X-Shopify-Hmac-Sha256") String hmac,
                                 @RequestHeader(value = "X-Shopify-Webhook-Id", required = false) String webhookId)
            throws IOException {
        requireConfigured();
        if (!ShopifySignatures.verifyWebhook(body, hmac, properties.apiSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (webhookId != null && !seenWebhooks.add(webhookId)) {
            return ResponseEntity.ok().build();
        }
        ShopView shop = tenancy.findByShopDomain(shopDomain).orElse(null);
        if (shop == null) {
            return ResponseEntity.ok().build();
        }
        JsonNode payload = json.readTree(body);
        switch (topic) {
            case "orders/create", "orders/updated" -> {
                var upsert = ShopifyOrderMapper.fromWebhook(payload);
                TenantContext.runAs(shop.id(), () -> catalog.upsert(upsert));
            }
            case "app/uninstalled" -> tenancy.markUninstalled(shop.id());
            // Webhooks RGPD obligatoires : Loopa ne garde que les données des commandes ayant fait l'objet
            // d'un retour. TODO : anonymiser les commandes et retours du client sur customers/redact.
            case "customers/data_request", "customers/redact", "shop/redact" ->
                    log.info("Webhook RGPD {} reçu pour {}", topic, shopDomain);
            default -> log.debug("Webhook ignoré : {}", topic);
        }
        return ResponseEntity.ok().build();
    }

    /* ---------- Admin ---------- */

    record IntegrationStatus(String platform, String shopDomain, boolean connected, boolean appConfigured,
                             long ordersSynced, Instant lastSyncAt) {
    }

    record SyncResult(int imported) {
    }

    @GetMapping("/api/admin/v1/integrations")
    IntegrationStatus status() {
        ShopView shop = tenancy.findById(Principals.currentShopId()).orElseThrow();
        boolean connected = "SHOPIFY".equals(shop.platform()) && tenancy.shopifyAccessToken(shop.id()).isPresent();
        return new IntegrationStatus(shop.platform(), shop.shopDomain(), connected, properties.configured(),
                catalog.count(), shopify.lastSyncAt(shop.id()));
    }

    @PostMapping("/api/admin/v1/integrations/shopify/sync")
    SyncResult sync() {
        return new SyncResult(shopify.syncOrders(Principals.currentShopId(), Duration.ofDays(60)));
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw new BusinessException("SHOPIFY_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE,
                    "L'application Shopify n'est pas configurée (LOOPA_SHOPIFY_API_KEY / API_SECRET).");
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
