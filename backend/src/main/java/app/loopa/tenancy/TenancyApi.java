package app.loopa.tenancy;

import java.util.Optional;
import java.util.UUID;

/** Façade publique du module tenancy. */
public interface TenancyApi {

    Optional<ShopView> findById(UUID shopId);

    Optional<ShopView> findByHandle(String handle);

    Optional<ShopView> findByShopDomain(String shopDomain);

    /** Crée (ou réactive) la boutique Shopify et son propriétaire à l'installation de l'app. */
    ShopView registerShopifyShop(String shopDomain, String name, String currency, String ownerEmail,
                                 String accessToken);

    /** Jeton d'accès Shopify déchiffré, jamais exposé par l'API HTTP. */
    Optional<String> shopifyAccessToken(UUID shopId);

    void markUninstalled(UUID shopId);

    void changePlan(UUID shopId, String plan);

    /** Emails des membres actifs de l'équipe (notification des nouvelles demandes). */
    java.util.List<String> teamEmails(UUID shopId);
}
