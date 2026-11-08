package app.loopa.returns.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** Journal immuable d'un retour : alimente la timeline de l'admin et du portail, et sert d'audit. */
@Entity
@Table(name = "return_event")
class ReturnHistoryEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    UUID tenantId;

    @Column(name = "return_request_id", nullable = false, updatable = false)
    UUID returnRequestId;

    @Column(nullable = false, updatable = false)
    String type;

    @Column(nullable = false, updatable = false)
    String actor;

    @Column(updatable = false)
    String message;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    Instant occurredAt;

    protected ReturnHistoryEntity() {
    }

    ReturnHistoryEntity(UUID tenantId, UUID returnRequestId, String type, String actor, String message,
                        Instant occurredAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.returnRequestId = returnRequestId;
        this.type = type;
        this.actor = actor;
        this.message = message;
        this.occurredAt = occurredAt;
    }
}
