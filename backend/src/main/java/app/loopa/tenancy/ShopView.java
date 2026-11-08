package app.loopa.tenancy;

import java.util.UUID;

/** Vue publique d'une boutique (tenant), partagée avec les autres modules. Ne contient aucun secret. */
public record ShopView(
        UUID id,
        String handle,
        String name,
        String platform,
        String shopDomain,
        String currency,
        String defaultLocale,
        String plan,
        String primaryColor,
        String logoUrl,
        String welcomeMessage,
        boolean active) {
}
