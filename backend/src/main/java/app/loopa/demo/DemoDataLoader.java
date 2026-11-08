package app.loopa.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insère la boutique de démonstration au démarrage si la base est vide.
 * Les dates du fichier sont relatives (« il y a N minutes ») : la démo est toujours à jour.
 * Comptes : yasmine@atlas-store.ma (propriétaire), salma@… (admin), karim@… (agent), mot de passe du fichier.
 */
@Component
@ConditionalOnProperty(name = "loopa.demo.enabled", havingValue = "true")
class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    DemoDataLoader(JdbcTemplate jdbc, ObjectMapper json, PasswordEncoder passwordEncoder, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Integer shops = jdbc.queryForObject("select count(*) from shop", Integer.class);
        if (shops != null && shops > 0) {
            return;
        }
        JsonNode seed;
        try (InputStream in = new ClassPathResource("demo/seed.json").getInputStream()) {
            seed = json.readTree(in);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MINUTES);
        JsonNode shop = seed.path("shop");
        UUID shopId = UUID.fromString(shop.path("id").asText());

        jdbc.update("""
                insert into shop (id, handle, name, platform, currency, default_locale, plan, primary_color, logo_url,
                                  welcome_message, active, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, ?)""",
                shopId, shop.path("handle").asText(), shop.path("name").asText(), shop.path("platform").asText(),
                shop.path("currency").asText(), shop.path("defaultLocale").asText(), shop.path("plan").asText(),
                shop.path("primaryColor").asText(), text(shop, "logoUrl"), text(shop, "welcomeMessage"),
                ts(now.minus(90, ChronoUnit.DAYS)));

        String hash = passwordEncoder.encode(seed.path("password").asText());
        for (JsonNode u : seed.path("users")) {
            jdbc.update("""
                    insert into shop_user (id, shop_id, email, name, password_hash, role, active, created_at)
                    values (?, ?, ?, ?, ?, ?, true, ?)""",
                    UUID.fromString(u.path("id").asText()), shopId, u.path("email").asText(), u.path("name").asText(),
                    hash, u.path("role").asText(), ts(now.minus(90, ChronoUnit.DAYS)));
        }

        JsonNode p = seed.path("policy");
        jdbc.update("""
                insert into return_policy (id, tenant_id, window_days, auto_approve, auto_approve_max_minor, allow_refund,
                                           allow_exchange, allow_store_credit, store_credit_bonus_minor,
                                           excluded_product_types)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID(), shopId, p.path("windowDays").asInt(), p.path("autoApprove").asBoolean(),
                p.path("autoApproveMaxAmountMinor").asLong(), p.path("allowRefund").asBoolean(),
                p.path("allowExchange").asBoolean(), p.path("allowStoreCredit").asBoolean(),
                p.path("storeCreditBonusMinor").asLong(), join(p.path("excludedProductTypes")));

        int position = 0;
        for (JsonNode r : seed.path("reasons")) {
            jdbc.update("""
                    insert into return_reason (id, tenant_id, code, label_fr, label_en, label_ar, requires_photo, active,
                                               position)
                    values (?, ?, ?, ?, ?, ?, ?, true, ?)""",
                    UUID.randomUUID(), shopId, r.path("code").asText(), r.path("labelFr").asText(),
                    r.path("labelEn").asText(), r.path("labelAr").asText(), r.path("requiresPhoto").asBoolean(),
                    position++);
        }

        for (JsonNode o : seed.path("orders")) {
            UUID orderId = UUID.fromString(o.path("id").asText());
            jdbc.update("""
                    insert into order_snapshot (id, tenant_id, external_id, name, name_normalized, customer_name,
                                                customer_email, customer_phone, currency, total_minor, payment_gateway,
                                                ordered_at, fulfilled_at, synced_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    orderId, shopId, o.path("externalId").asText(), o.path("name").asText(),
                    o.path("name").asText().replaceAll("[^0-9A-Za-z]", "").toUpperCase(),
                    text(o, "customerName"), text(o, "customerEmail"),
                    text(o, "customerPhone") == null ? null : o.path("customerPhone").asText().replaceAll("\\D", ""),
                    o.path("currency").asText(), o.path("totalMinor").asLong(), text(o, "paymentGateway"),
                    ago(now, o, "orderedMinutesAgo"), ago(now, o, "fulfilledMinutesAgo"), ts(now));
            int linePos = 0;
            for (JsonNode l : o.path("lines")) {
                jdbc.update("""
                        insert into order_line_snapshot (id, tenant_id, order_id, position, external_line_id, product_id,
                                                         variant_id, title, variant_title, sku, product_type, quantity,
                                                         unit_price_minor, image_url)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                        UUID.randomUUID(), shopId, orderId, linePos++, l.path("externalLineId").asText(),
                        text(l, "productId"), text(l, "variantId"), l.path("title").asText(), text(l, "variantTitle"),
                        text(l, "sku"), text(l, "productType"), l.path("quantity").asInt(),
                        l.path("unitPriceMinor").asLong(), text(l, "imageUrl"));
            }
        }

        int maxNumber = 0;
        for (JsonNode r : seed.path("returns")) {
            UUID returnId = UUID.fromString(r.path("id").asText());
            Timestamp created = ago(now, r, "createdMinutesAgo");
            Timestamp approved = r.path("approvedAfterMinutes").isNull() ? null
                    : Timestamp.from(created.toInstant().plus(r.path("approvedAfterMinutes").asLong(), ChronoUnit.MINUTES));
            jdbc.update("""
                    insert into return_request (id, tenant_id, number, order_id, order_name, customer_name, customer_email,
                                                locale, status, resolution_type, customer_note, bonus_minor,
                                                rejection_reason, carrier, tracking_number, resolution_ref,
                                                created_at, approved_at, updated_at, version)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)""",
                    returnId, shopId, r.path("number").asText(), UUID.fromString(r.path("orderId").asText()),
                    r.path("orderName").asText(), text(r, "customerName"), text(r, "customerEmail"),
                    r.path("locale").asText(), r.path("status").asText(), r.path("resolutionType").asText(),
                    text(r, "customerNote"), r.path("bonusMinor").asLong(), text(r, "rejectionReason"),
                    text(r, "carrier"), text(r, "trackingNumber"), text(r, "resolutionRef"),
                    created, approved, ago(now, r, "updatedMinutesAgo"));
            int linePos = 0;
            for (JsonNode l : r.path("lines")) {
                jdbc.update("""
                        insert into return_line (id, tenant_id, return_request_id, position, order_line_id, title,
                                                 variant_title, quantity, unit_price_minor, currency, reason_code,
                                                 exchange_variant_id)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                        UUID.randomUUID(), shopId, returnId, linePos++, l.path("orderLineId").asText(),
                        l.path("title").asText(), text(l, "variantTitle"), l.path("quantity").asInt(),
                        l.path("unitPriceMinor").asLong(), l.path("currency").asText(), l.path("reasonCode").asText(),
                        text(l, "exchangeVariantId"));
            }
            for (JsonNode h : r.path("history")) {
                jdbc.update("""
                        insert into return_event (id, tenant_id, return_request_id, type, actor, message, occurred_at)
                        values (?, ?, ?, ?, ?, ?, ?)""",
                        UUID.randomUUID(), shopId, returnId, h.path("type").asText(), h.path("actor").asText(),
                        text(h, "message"), ago(now, h, "minutesAgo"));
            }
            String number = r.path("number").asText();
            maxNumber = Math.max(maxNumber, Integer.parseInt(number.substring(number.lastIndexOf('-') + 1)));
        }
        jdbc.queryForObject("select setval('return_number_seq', ?)", Long.class, (long) maxNumber);
        log.info("Boutique de démonstration chargée : {} commandes, {} retours. Connexion : {} / {}",
                seed.path("orders").size(), seed.path("returns").size(),
                seed.path("users").get(0).path("email").asText(), seed.path("password").asText());
    }

    private static Timestamp ago(Instant now, JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : Timestamp.from(now.minus(v.asLong(), ChronoUnit.MINUTES));
    }

    private static Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static String join(JsonNode array) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode n : array) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(n.asText());
        }
        return sb.toString();
    }
}
