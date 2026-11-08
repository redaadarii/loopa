package app.loopa.tenancy.internal;

import app.loopa.shared.BusinessException;
import app.loopa.tenancy.ShopView;
import app.loopa.tenancy.TenancyApi;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TenancyService implements TenancyApi {

    private final ShopRepository shops;
    private final ShopUserRepository users;
    private final SecretCipher cipher;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    TenancyService(ShopRepository shops, ShopUserRepository users, SecretCipher cipher,
                   PasswordEncoder passwordEncoder, Clock clock) {
        this.shops = shops;
        this.users = users;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopView> findById(UUID shopId) {
        return shops.findById(shopId).map(ShopEntity::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopView> findByHandle(String handle) {
        return shops.findByHandle(handle.toLowerCase(Locale.ROOT)).map(ShopEntity::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShopView> findByShopDomain(String shopDomain) {
        return shops.findByShopDomain(shopDomain.toLowerCase(Locale.ROOT)).map(ShopEntity::toView);
    }

    @Override
    @Transactional
    public ShopView registerShopifyShop(String shopDomain, String name, String currency, String ownerEmail,
                                        String accessToken) {
        String domain = shopDomain.toLowerCase(Locale.ROOT);
        ShopEntity shop = shops.findByShopDomain(domain).orElseGet(() -> {
            ShopEntity s = new ShopEntity();
            s.id = UUID.randomUUID();
            s.handle = uniqueHandle(domain.replace(".myshopify.com", ""));
            s.platform = "SHOPIFY";
            s.shopDomain = domain;
            s.defaultLocale = "fr";
            s.plan = "STARTER";
            s.primaryColor = "#0E7C6B";
            s.createdAt = clock.instant();
            return s;
        });
        shop.name = name;
        shop.currency = currency;
        shop.accessTokenEncrypted = cipher.encrypt(accessToken);
        shop.active = true;
        shop.uninstalledAt = null;
        shops.save(shop);

        if (ownerEmail != null && !users.existsByEmailIgnoreCase(ownerEmail)) {
            ShopUserEntity owner = new ShopUserEntity();
            owner.id = UUID.randomUUID();
            owner.shopId = shop.id;
            owner.email = ownerEmail.toLowerCase(Locale.ROOT);
            owner.name = name;
            // Mot de passe aléatoire : le propriétaire se connecte depuis l'admin Shopify ou le réinitialise.
            owner.passwordHash = passwordEncoder.encode(randomSecret());
            owner.role = ShopUserEntity.Role.OWNER;
            owner.active = true;
            owner.createdAt = clock.instant();
            users.save(owner);
        }
        return shop.toView();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> shopifyAccessToken(UUID shopId) {
        return shops.findById(shopId)
                .filter(s -> s.accessTokenEncrypted != null)
                .map(s -> cipher.decrypt(s.accessTokenEncrypted));
    }

    @Override
    @Transactional
    public void markUninstalled(UUID shopId) {
        shops.findById(shopId).ifPresent(s -> {
            s.active = false;
            s.accessTokenEncrypted = null;
            s.uninstalledAt = clock.instant();
        });
    }

    @Override
    @Transactional
    public void changePlan(UUID shopId, String plan) {
        ShopEntity shop = shops.findById(shopId).orElseThrow(() -> BusinessException.notFound("Boutique", shopId));
        shop.plan = plan;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<String> teamEmails(UUID shopId) {
        return users.findByShopIdOrderByCreatedAt(shopId).stream().filter(u -> u.active).map(u -> u.email).toList();
    }

    String randomSecret() {
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String uniqueHandle(String base) {
        String cleaned = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        String candidate = cleaned;
        int i = 2;
        while (shops.existsByHandle(candidate)) {
            candidate = cleaned + "-" + i++;
        }
        return candidate;
    }
}
