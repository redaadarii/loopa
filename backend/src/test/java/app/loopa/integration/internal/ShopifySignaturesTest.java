package app.loopa.integration.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShopifySignaturesTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    @Test
    void verifiesOfficialOAuthExampleFromShopifyDocs() {
        Map<String, String> params = new HashMap<>(Map.of(
                "code", "0907a61c0c8d55e99db179b68161bc00",
                "shop", "some-shop.myshopify.com",
                "state", "0.6784241404160823",
                "timestamp", "1337178173",
                "hmac", "700e2dadb827fcc8609e9d5ce208b2e9cdaab9df07390d2cbca10d7c328fc4bf"));

        assertThat(ShopifySignatures.verifyQuery(params, "hush")).isTrue();
        params.put("shop", "evil-shop.myshopify.com");
        assertThat(ShopifySignatures.verifyQuery(params, "hush")).isFalse();
    }

    @Test
    void stateIsBoundToShopAndExpires() {
        String state = ShopifySignatures.signState("atlas.myshopify.com", NOW.plusSeconds(600), "secret");

        assertThat(ShopifySignatures.verifyState(state, "atlas.myshopify.com", NOW, "secret")).isTrue();
        assertThat(ShopifySignatures.verifyState(state, "other.myshopify.com", NOW, "secret")).isFalse();
        assertThat(ShopifySignatures.verifyState(state, "atlas.myshopify.com", NOW.plusSeconds(601), "secret")).isFalse();
        assertThat(ShopifySignatures.verifyState(state + "0", "atlas.myshopify.com", NOW, "secret")).isFalse();
    }

    @Test
    void webhookSignatureCoversRawBody() {
        byte[] body = "{\"id\":1}".getBytes(StandardCharsets.UTF_8);
        // HMAC-SHA256 Base64 de {"id":1} avec la clé « secret »
        String hmac = java.util.Base64.getEncoder().encodeToString(hmac("secret", body));

        assertThat(ShopifySignatures.verifyWebhook(body, hmac, "secret")).isTrue();
        assertThat(ShopifySignatures.verifyWebhook("{\"id\":2}".getBytes(StandardCharsets.UTF_8), hmac, "secret"))
                .isFalse();
        assertThat(ShopifySignatures.verifyWebhook(body, null, "secret")).isFalse();
    }

    @Test
    void rejectsShopDomainsOutsideMyshopify() {
        assertThat(ShopifySignatures.isValidShopDomain("atlas-store.myshopify.com")).isTrue();
        assertThat(ShopifySignatures.isValidShopDomain("evil.com/x.myshopify.com")).isFalse();
        assertThat(ShopifySignatures.isValidShopDomain("atlas.myshopify.com.evil.com")).isFalse();
    }

    private static byte[] hmac(String key, byte[] body) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(body);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
