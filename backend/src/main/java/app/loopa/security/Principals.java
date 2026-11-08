package app.loopa.security;

import app.loopa.shared.BusinessException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Lecture typée de l'utilisateur courant depuis le jeton. */
public final class Principals {

    public static final String SHOP_CLAIM = "shop_id";
    public static final String ORDER_CLAIM = "order_id";

    private Principals() {
    }

    private static Optional<Jwt> jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }

    private static Jwt requireJwt() {
        return jwt().orElseThrow(() ->
                new BusinessException("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Authentification requise"));
    }

    /** Identifiant de l'utilisateur marchand connecté. */
    public static UUID currentUserId() {
        return UUID.fromString(requireJwt().getSubject());
    }

    public static UUID currentShopId() {
        return UUID.fromString(requireJwt().getClaimAsString(SHOP_CLAIM));
    }

    /** Commande à laquelle le jeton portail donne accès. */
    public static UUID portalOrderId() {
        String value = requireJwt().getClaimAsString(ORDER_CLAIM);
        if (value == null) {
            throw new BusinessException("FORBIDDEN", HttpStatus.FORBIDDEN, "Jeton portail requis");
        }
        return UUID.fromString(value);
    }

    /** Nom affiché dans l'historique des retours pour l'utilisateur connecté. */
    public static String currentActorName() {
        Jwt jwt = requireJwt();
        String name = jwt.getClaimAsString("name");
        return name != null ? name : jwt.getClaimAsString("email");
    }

    public static String portalContact() {
        return requireJwt().getSubject();
    }

    static Optional<UUID> shopIdIfPresent() {
        return jwt().filter(j -> j.hasClaim(SHOP_CLAIM)).map(j -> UUID.fromString(j.getClaimAsString(SHOP_CLAIM)));
    }
}
