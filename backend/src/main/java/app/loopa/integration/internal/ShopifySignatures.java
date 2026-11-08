package app.loopa.integration.internal;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Vérifications cryptographiques imposées par Shopify (OAuth, webhooks, paramètre state). */
final class ShopifySignatures {

    private static final Pattern SHOP_DOMAIN = Pattern.compile("^[a-z0-9][a-z0-9-]*\\.myshopify\\.com$");

    private ShopifySignatures() {
    }

    static boolean isValidShopDomain(String shop) {
        return shop != null && SHOP_DOMAIN.matcher(shop).matches();
    }

    /** Paramètres de requête OAuth : HMAC hexadécimal des paires triées, hors « hmac ». */
    static boolean verifyQuery(Map<String, String> params, String secret) {
        String received = params.get("hmac");
        if (received == null) {
            return false;
        }
        String message = new TreeMap<>(params).entrySet().stream()
                .filter(e -> !"hmac".equals(e.getKey()) && !"signature".equals(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        String expected = HexFormat.of().formatHex(hmac(secret, message.getBytes(StandardCharsets.UTF_8)));
        return constantTimeEquals(expected, received);
    }

    /** Webhooks : HMAC Base64 du corps brut (en-tête X-Shopify-Hmac-Sha256). */
    static boolean verifyWebhook(byte[] body, String headerHmac, String secret) {
        if (headerHmac == null) {
            return false;
        }
        String expected = Base64.getEncoder().encodeToString(hmac(secret, body));
        return constantTimeEquals(expected, headerHmac);
    }

    /** {@code state} OAuth sans stockage : « domaine.expiration.signature ». */
    static String signState(String shop, Instant expiresAt, String secret) {
        String payload = shop + "." + expiresAt.getEpochSecond();
        return payload + "." + HexFormat.of().formatHex(hmac(secret, payload.getBytes(StandardCharsets.UTF_8)));
    }

    static boolean verifyState(String state, String shop, Instant now, String secret) {
        if (state == null) {
            return false;
        }
        int last = state.lastIndexOf('.');
        int middle = state.lastIndexOf('.', last - 1);
        if (last < 0 || middle < 0) {
            return false;
        }
        String payload = state.substring(0, last);
        String stateShop = state.substring(0, middle);
        long expires;
        try {
            expires = Long.parseLong(state.substring(middle + 1, last));
        } catch (NumberFormatException e) {
            return false;
        }
        String expected = HexFormat.of().formatHex(hmac(secret, payload.getBytes(StandardCharsets.UTF_8)));
        return stateShop.equals(shop) && now.getEpochSecond() <= expires
                && constantTimeEquals(expected, state.substring(last + 1));
    }

    private static byte[] hmac(String secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
