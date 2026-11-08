package app.loopa.integration.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application Shopify ({@code loopa.shopify.*}).
 *
 * @param apiKey     « Client ID » de l'app (Partner Dashboard)
 * @param apiSecret  « Client secret » : signe l'OAuth et les webhooks (HMAC-SHA256)
 * @param scopes     autorisations demandées au marchand
 * @param apiUrl     URL publique de cette API (redirection OAuth, webhooks)
 * @param adminUrl   URL du back-office Nuxt, où le marchand arrive après l'installation
 * @param apiVersion version de l'API Admin GraphQL
 */
@ConfigurationProperties("loopa.shopify")
public record ShopifyProperties(String apiKey, String apiSecret, String scopes, String apiUrl, String adminUrl,
                         String apiVersion) {

    public ShopifyProperties {
        if (scopes == null) {
            scopes = "read_orders,write_orders,read_products,read_customers,write_returns,write_gift_cards,"
                    + "write_draft_orders";
        }
        if (apiVersion == null) {
            apiVersion = "2025-10";
        }
    }

    boolean configured() {
        return apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank();
    }
}
