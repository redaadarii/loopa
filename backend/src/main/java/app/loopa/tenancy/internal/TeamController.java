package app.loopa.tenancy.internal;

import app.loopa.security.Principals;
import app.loopa.shared.BusinessException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Gestion de l'équipe du marchand (réservée aux rôles owner et admin). */
@RestController
@RequestMapping("/api/admin/v1/team")
@Tag(name = "Admin - Équipe")
class TeamController {

    private final ShopUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TenancyService tenancy;
    private final Clock clock;

    TeamController(ShopUserRepository users, PasswordEncoder passwordEncoder, TenancyService tenancy, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tenancy = tenancy;
        this.clock = clock;
    }

    record MemberDto(UUID id, String email, String name, String role, boolean active, Instant lastLoginAt) {

        static MemberDto from(ShopUserEntity u) {
            return new MemberDto(u.id, u.email, u.name, u.role.name(), u.active, u.lastLoginAt);
        }
    }

    record InviteRequest(@NotBlank @Email String email, @NotBlank @Size(max = 120) String name,
                         @NotNull ShopUserEntity.Role role) {
    }

    record InviteResponse(MemberDto member, String temporaryPassword) {
    }

    record UpdateMemberRequest(ShopUserEntity.Role role, Boolean active) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    List<MemberDto> list() {
        return users.findByShopIdOrderByCreatedAt(Principals.currentShopId()).stream().map(MemberDto::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    InviteResponse invite(@Valid @RequestBody InviteRequest body) {
        if (body.role() == ShopUserEntity.Role.OWNER) {
            throw new BusinessException("OWNER_UNIQUE", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Une boutique n'a qu'un propriétaire.");
        }
        if (users.existsByEmailIgnoreCase(body.email())) {
            throw new BusinessException("EMAIL_TAKEN", HttpStatus.CONFLICT, "Cet email est déjà utilisé.");
        }
        String temporaryPassword = tenancy.randomSecret();
        ShopUserEntity u = new ShopUserEntity();
        u.id = UUID.randomUUID();
        u.shopId = Principals.currentShopId();
        u.email = body.email().trim().toLowerCase(Locale.ROOT);
        u.name = body.name().trim();
        u.passwordHash = passwordEncoder.encode(temporaryPassword);
        u.role = body.role();
        u.active = true;
        u.createdAt = clock.instant();
        users.save(u);
        // TODO : envoyer l'invitation par email (module notification) au lieu de renvoyer le mot de passe.
        return new InviteResponse(MemberDto.from(u), temporaryPassword);
    }

    @PatchMapping("/{id}")
    @Transactional
    MemberDto update(@PathVariable UUID id, @RequestBody UpdateMemberRequest body) {
        ShopUserEntity u = users.findByIdAndShopId(id, Principals.currentShopId())
                .orElseThrow(() -> BusinessException.notFound("Membre", id));
        if (u.role == ShopUserEntity.Role.OWNER) {
            throw new BusinessException("OWNER_LOCKED", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Le propriétaire ne peut pas être modifié.");
        }
        if (body.role() != null && body.role() != ShopUserEntity.Role.OWNER) {
            u.role = body.role();
        }
        if (body.active() != null) {
            u.active = body.active();
        }
        return MemberDto.from(u);
    }
}
