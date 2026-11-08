package app.loopa.security;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Émet les jetons de l'API. Le claim {@code scope} devient l'autorité {@code SCOPE_<x>} côté Spring Security.
 */
@Service
public class TokenService {

    public static final String ISSUER = "loopa";

    private final JwtEncoder encoder;
    private final SecurityProperties properties;
    private final Clock clock;

    TokenService(JwtEncoder encoder, SecurityProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    /** Jeton marchand. {@code scope} vaut {@code admin} (owner, admin) ou {@code agent}. */
    public IssuedToken issueMerchant(UUID userId, UUID shopId, String email, String name, String scope) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.adminTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(Principals.SHOP_CLAIM, shopId.toString())
                .claim("email", email)
                .claim("name", name)
                .claim("scope", scope)
                .build();
        return new IssuedToken(encode(claims), expiresAt);
    }

    /** Jeton client final : limité à une boutique ET à une commande. */
    public IssuedToken issuePortal(UUID shopId, UUID orderId, String contact) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.portalTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(contact)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(Principals.SHOP_CLAIM, shopId.toString())
                .claim(Principals.ORDER_CLAIM, orderId.toString())
                .claim("scope", "portal")
                .build();
        return new IssuedToken(encode(claims), expiresAt);
    }

    private String encode(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }
}
