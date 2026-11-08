package app.loopa.tenancy.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shop_user")
class ShopUserEntity {

    enum Role {
        OWNER, ADMIN, AGENT;

        /** Scope du jeton : owner et admin gèrent tout, agent traite les retours. */
        String scope() {
            return this == AGENT ? "agent" : "admin";
        }
    }

    @Id
    UUID id;

    @Column(name = "shop_id", nullable = false)
    UUID shopId;

    @Column(nullable = false, unique = true)
    String email;

    @Column(nullable = false)
    String name;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Role role;

    @Column(nullable = false)
    boolean active;

    @Column(name = "last_login_at")
    Instant lastLoginAt;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected ShopUserEntity() {
    }
}
