package app.loopa.integration.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Appels HTTP à Shopify : échange du code OAuth et API Admin GraphQL. */
@Component
class ShopifyClient {

    private final RestClient http;
    private final ObjectMapper json;
    private final ShopifyProperties properties;

    ShopifyClient(RestClient.Builder builder, ObjectMapper json, ShopifyProperties properties) {
        this.http = builder.build();
        this.json = json;
        this.properties = properties;
    }

    /** Échange le code d'autorisation contre un jeton d'accès hors ligne. */
    String exchangeCode(String shop, String code) {
        JsonNode response = http.post()
                .uri("https://{shop}/admin/oauth/access_token", shop)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("client_id", properties.apiKey(), "client_secret", properties.apiSecret(), "code", code))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.hasNonNull("access_token")) {
            throw new IllegalStateException("Réponse OAuth Shopify invalide");
        }
        return response.get("access_token").asText();
    }

    /**
     * Exécute une requête GraphQL et renvoie {@code data}. Lève une exception si Shopify renvoie des
     * {@code errors} ou des {@code userErrors} au premier niveau de la mutation.
     */
    JsonNode graphql(String shop, String accessToken, String query, Map<String, Object> variables) {
        ObjectNode body = json.createObjectNode();
        body.put("query", query);
        body.set("variables", json.valueToTree(variables));
        JsonNode response = http.post()
                .uri("https://{shop}/admin/api/{version}/graphql.json", shop, properties.apiVersion())
                .header("X-Shopify-Access-Token", accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (response == null) {
            throw new IllegalStateException("Réponse Shopify vide");
        }
        if (response.has("errors")) {
            throw new IllegalStateException("Shopify : " + response.get("errors"));
        }
        JsonNode data = response.path("data");
        data.fields().forEachRemaining(field -> {
            JsonNode userErrors = field.getValue().path("userErrors");
            if (userErrors.isArray() && !userErrors.isEmpty()) {
                throw new IllegalStateException("Shopify : " + userErrors.get(0).path("message").asText());
            }
        });
        return data;
    }
}
