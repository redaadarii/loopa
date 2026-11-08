package app.loopa.tenancy.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ShopRepository extends JpaRepository<ShopEntity, UUID> {

    Optional<ShopEntity> findByHandle(String handle);

    Optional<ShopEntity> findByShopDomain(String shopDomain);

    boolean existsByHandle(String handle);
}

interface ShopUserRepository extends JpaRepository<ShopUserEntity, UUID> {

    Optional<ShopUserEntity> findByEmailIgnoreCase(String email);

    List<ShopUserEntity> findByShopIdOrderByCreatedAt(UUID shopId);

    Optional<ShopUserEntity> findByIdAndShopId(UUID id, UUID shopId);

    boolean existsByEmailIgnoreCase(String email);
}
