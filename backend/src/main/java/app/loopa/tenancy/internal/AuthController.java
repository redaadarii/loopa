package app.loopa.tenancy.internal;

import app.loopa.security.Principals;
import app.loopa.security.TokenService;
import app.loopa.shared.BusinessException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connexion des marchands (email + mot de passe) et profil courant. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification")
class AuthController {

    private final ShopUserRepository users;
    private final ShopRepository shops;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    private final Clock clock;

    AuthController(ShopUserRepository users, ShopRepository shops, PasswordEncoder passwordEncoder,
                   TokenService tokens, Clock clock) {
        this.users = users;
        this.shops = shops;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.clock = clock;
    }

    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    record UserDto(UUID id, String email, String name, String role) {
    }

    record ShopDto(UUID id, String handle, String name, String currency, String plan, String platform) {
    }

    record SessionResponse(String token, Instant expiresAt, UserDto user, ShopDto shop) {
    }

    @PostMapping("/login")
    @Transactional
    SessionResponse login(@Valid @RequestBody LoginRequest body) {
        ShopUserEntity user = users.findByEmailIgnoreCase(body.email().trim())
                .filter(u -> u.active)
                .filter(u -> passwordEncoder.matches(body.password(), u.passwordHash))
                .orElseThrow(() -> new BusinessException("BAD_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                        "Email ou mot de passe incorrect."));
        ShopEntity shop = shops.findById(user.shopId).filter(s -> s.active)
                .orElseThrow(() -> new BusinessException("SHOP_INACTIVE", HttpStatus.FORBIDDEN,
                        "Cette boutique est désactivée."));
        user.lastLoginAt = clock.instant();
        TokenService.IssuedToken token = tokens.issueMerchant(user.id, shop.id, user.email, user.name,
                user.role.scope());
        return new SessionResponse(token.value(), token.expiresAt(), toDto(user), toDto(shop));
    }

    record ChangePasswordRequest(@NotBlank String currentPassword,
                                 @NotBlank @jakarta.validation.constraints.Size(min = 10, max = 128) String newPassword) {
    }

    /** Changement du mot de passe (obligatoire après une invitation avec mot de passe temporaire). */
    @org.springframework.web.bind.annotation.PostMapping("/password")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void changePassword(@Valid @RequestBody ChangePasswordRequest body) {
        ShopUserEntity user = users.findById(Principals.currentUserId())
                .orElseThrow(() -> BusinessException.notFound("Utilisateur", Principals.currentUserId()));
        if (!passwordEncoder.matches(body.currentPassword(), user.passwordHash)) {
            throw new BusinessException("BAD_CREDENTIALS", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Le mot de passe actuel est incorrect.");
        }
        if (body.currentPassword().equals(body.newPassword())) {
            throw new BusinessException("SAME_PASSWORD", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Le nouveau mot de passe doit être différent de l'actuel.");
        }
        user.passwordHash = passwordEncoder.encode(body.newPassword());
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    SessionResponse me() {
        ShopUserEntity user = users.findById(Principals.currentUserId())
                .orElseThrow(() -> BusinessException.notFound("Utilisateur", Principals.currentUserId()));
        ShopEntity shop = shops.findById(user.shopId).orElseThrow();
        return new SessionResponse(null, null, toDto(user), toDto(shop));
    }

    static UserDto toDto(ShopUserEntity u) {
        return new UserDto(u.id, u.email, u.name, u.role.name());
    }

    static ShopDto toDto(ShopEntity s) {
        return new ShopDto(s.id, s.handle, s.name, s.currency, s.plan, s.platform);
    }
}
