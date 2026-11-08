package app.loopa.security;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de sécurité ({@code loopa.security.*}).
 *
 * @param jwtSecret      secret HMAC des jetons (≥ 32 octets, variable LOOPA_JWT_SECRET en production)
 * @param adminTokenTtl  durée de vie d'un jeton marchand
 * @param portalTokenTtl durée de vie d'un jeton portail (obtenu après OTP, limité à une commande)
 * @param corsOrigins    origines autorisées pour les fronts Nuxt
 */
@ConfigurationProperties("loopa.security")
public record SecurityProperties(
        String jwtSecret,
        Duration adminTokenTtl,
        Duration portalTokenTtl,
        List<String> corsOrigins) {

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("loopa.security.jwt-secret doit faire au moins 32 octets");
        }
        if (adminTokenTtl == null) {
            adminTokenTtl = Duration.ofHours(8);
        }
        if (portalTokenTtl == null) {
            portalTokenTtl = Duration.ofMinutes(30);
        }
        if (corsOrigins == null) {
            corsOrigins = List.of();
        }
    }
}
