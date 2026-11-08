package app.loopa.tenancy.internal;

import app.loopa.tenancy.ShopView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Une boutique = un tenant. Pas de {@code @TenantId} ici : c'est la table des tenants eux-mêmes. */
@Entity
@Table(name = "shop")
class ShopEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true)
    String handle;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String platform;

    @Column(name = "shop_domain", unique = true)
    String shopDomain;

    @Column(name = "access_token_enc")
    String accessTokenEncrypted;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(name = "default_locale", nullable = false)
    String defaultLocale;

    @Column(nullable = false)
    String plan;

    @Column(name = "primary_color", nullable = false)
    String primaryColor;

    @Column(name = "logo_url")
    String logoUrl;

    @Column(name = "welcome_message")
    String welcomeMessage;

    @Column(nullable = false)
    boolean active;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "uninstalled_at")
    Instant uninstalledAt;

    protected ShopEntity() {
    }

    ShopView toView() {
        return new ShopView(id, handle, name, platform, shopDomain, currency, defaultLocale, plan, primaryColor,
                logoUrl, welcomeMessage, active);
    }
}
