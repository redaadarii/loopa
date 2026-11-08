package app.loopa.returns.adapter.out.persistence;

import app.loopa.returns.domain.ResolutionType;
import app.loopa.returns.domain.ReturnStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "return_request")
class ReturnRequestEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    UUID tenantId;

    @Column(nullable = false, unique = true)
    String number;

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(name = "order_name")
    String orderName;

    @Column(name = "customer_name")
    String customerName;

    @Column(name = "customer_email")
    String customerEmail;

    @Column(nullable = false, length = 5)
    String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ReturnStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", nullable = false)
    ResolutionType resolutionType;

    @Column(name = "customer_note")
    String customerNote;

    @Column(name = "bonus_minor", nullable = false)
    long bonusMinor;

    @Column(name = "rejection_reason")
    String rejectionReason;

    String carrier;

    @Column(name = "tracking_number")
    String trackingNumber;

    @Column(name = "resolution_ref")
    String resolutionRef;

    @Column(name = "resolution_error")
    String resolutionError;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "approved_at")
    Instant approvedAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Version
    Long version;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @BatchSize(size = 50)
    List<ReturnLineEntity> lines = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "return_attachment", joinColumns = @JoinColumn(name = "return_request_id"))
    @Column(name = "file_id", nullable = false)
    @OrderColumn(name = "position")
    @BatchSize(size = 50)
    List<UUID> attachmentIds = new ArrayList<>();

    protected ReturnRequestEntity() {
    }
}
