package app.loopa.tenancy.internal;

import app.loopa.security.Principals;
import app.loopa.shared.BusinessException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Personnalisation du portail client : nom affiché, couleur, logo, message d'accueil. */
@RestController
@RequestMapping("/api/admin/v1/settings/branding")
@Tag(name = "Admin - Personnalisation")
class BrandingController {

    private final ShopRepository shops;

    BrandingController(ShopRepository shops) {
        this.shops = shops;
    }

    record BrandingDto(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String primaryColor,
            @Size(max = 500) String logoUrl,
            @Size(max = 280) String welcomeMessage,
            String handle) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    BrandingDto get() {
        ShopEntity s = current();
        return new BrandingDto(s.name, s.primaryColor, s.logoUrl, s.welcomeMessage, s.handle);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    @Transactional
    BrandingDto update(@Valid @RequestBody BrandingDto body) {
        ShopEntity s = current();
        s.name = body.name().trim();
        s.primaryColor = body.primaryColor().toUpperCase(java.util.Locale.ROOT);
        s.logoUrl = blankToNull(body.logoUrl());
        s.welcomeMessage = blankToNull(body.welcomeMessage());
        return new BrandingDto(s.name, s.primaryColor, s.logoUrl, s.welcomeMessage, s.handle);
    }

    private ShopEntity current() {
        return shops.findById(Principals.currentShopId())
                .orElseThrow(() -> BusinessException.notFound("Boutique", Principals.currentShopId()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
