package app.loopa.portal.internal;

import app.loopa.catalog.CatalogApi;
import app.loopa.catalog.OrderView;
import app.loopa.notification.NotificationApi;
import app.loopa.security.TokenService;
import app.loopa.shared.BusinessException;
import app.loopa.shared.tenant.TenantContext;
import app.loopa.tenancy.ShopView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Code à usage unique. Pas de {@code @TenantId} : la vérification arrive sans contexte de boutique. */
@Entity
@Table(name = "portal_otp")
class OtpEntity {

    @Id
    UUID id;

    @Column(name = "tenant_id", nullable = false)
    UUID tenantId;

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(nullable = false)
    String contact;

    @Column(name = "code_hash", nullable = false)
    String codeHash;

    @Column(nullable = false)
    int attempts;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "consumed_at")
    Instant consumedAt;

    protected OtpEntity() {
    }
}

interface OtpRepository extends JpaRepository<OtpEntity, UUID> {

    long countByOrderIdAndCreatedAtAfter(UUID orderId, Instant after);
}

/**
 * Identification du client final sans compte : numéro de commande + email/téléphone, puis code à 6 chiffres.
 * Réponses volontairement identiques quand la commande ou le contact ne correspondent pas.
 */
@Service
class PortalSessionService {

    static final Duration CODE_TTL = Duration.ofMinutes(10);
    static final int MAX_ATTEMPTS = 5;
    static final int MAX_CODES_PER_HOUR = 5;

    private final CatalogApi catalog;
    private final NotificationApi notifications;
    private final TokenService tokens;
    private final OtpRepository otps;
    private final Clock clock;
    private final boolean exposeCode;
    private final SecureRandom random = new SecureRandom();

    PortalSessionService(CatalogApi catalog, NotificationApi notifications, TokenService tokens, OtpRepository otps,
                         Clock clock, @Value("${loopa.portal.expose-code:false}") boolean exposeCode) {
        this.catalog = catalog;
        this.notifications = notifications;
        this.tokens = tokens;
        this.otps = otps;
        this.clock = clock;
        this.exposeCode = exposeCode;
    }

    record SessionStarted(UUID sessionId, String channel, String maskedContact, Instant expiresAt, String devCode) {
    }

    record SessionVerified(String token, Instant expiresAt, UUID orderId) {
    }

    /** À appeler dans {@code TenantContext.callAs(shop.id(), …)}. */
    @Transactional
    SessionStarted start(ShopView shop, String orderNumber, String contact, String locale) {
        OrderView order = catalog.findForCustomer(orderNumber, contact)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND,
                        "Aucune commande ne correspond à ce numéro et à ce contact."));
        Instant now = clock.instant();
        if (otps.countByOrderIdAndCreatedAtAfter(order.id(), now.minus(Duration.ofHours(1))) >= MAX_CODES_PER_HOUR) {
            throw new BusinessException("TOO_MANY_CODES", HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de codes demandés. Réessayez dans une heure.");
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        OtpEntity otp = new OtpEntity();
        otp.id = UUID.randomUUID();
        otp.tenantId = TenantContext.require();
        otp.orderId = order.id();
        otp.contact = contact.trim();
        otp.codeHash = hash(otp.id, code);
        otp.attempts = 0;
        otp.createdAt = now;
        otp.expiresAt = now.plus(CODE_TTL);
        otps.save(otp);

        notifications.sendPortalCode(shop.name(), otp.contact, code, locale);
        String channel = otp.contact.contains("@") ? "EMAIL" : "SMS";
        return new SessionStarted(otp.id, channel, mask(otp.contact), otp.expiresAt, exposeCode ? code : null);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    SessionVerified verify(UUID sessionId, String code) {
        OtpEntity otp = otps.findById(sessionId).orElseThrow(PortalSessionService::invalidCode);
        Instant now = clock.instant();
        if (otp.consumedAt != null || now.isAfter(otp.expiresAt)) {
            throw new BusinessException("CODE_EXPIRED", HttpStatus.GONE, "Ce code a expiré. Demandez-en un nouveau.");
        }
        if (otp.attempts >= MAX_ATTEMPTS) {
            throw new BusinessException("TOO_MANY_ATTEMPTS", HttpStatus.TOO_MANY_REQUESTS,
                    "Trop d'essais. Demandez un nouveau code.");
        }
        otp.attempts++;
        boolean ok = MessageDigest.isEqual(hash(otp.id, code == null ? "" : code.trim())
                .getBytes(StandardCharsets.UTF_8), otp.codeHash.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            throw invalidCode();
        }
        otp.consumedAt = now;
        TokenService.IssuedToken token = tokens.issuePortal(otp.tenantId, otp.orderId, otp.contact);
        return new SessionVerified(token.value(), token.expiresAt(), otp.orderId);
    }

    private static BusinessException invalidCode() {
        return new BusinessException("INVALID_CODE", HttpStatus.UNPROCESSABLE_ENTITY, "Code incorrect.");
    }

    static String hash(UUID salt, String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((salt + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String mask(String contact) {
        int at = contact.indexOf('@');
        if (at > 0) {
            return contact.charAt(0) + "•••" + contact.substring(at);
        }
        String digits = contact.replaceAll("\\D", "");
        return "•••• " + (digits.length() >= 2 ? digits.substring(digits.length() - 2) : "");
    }
}
