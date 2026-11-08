package app.loopa.policy.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "return_policy")
class PolicyEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false, unique = true)
    UUID tenantId;

    @Column(name = "window_days", nullable = false)
    int windowDays;

    @Column(name = "auto_approve", nullable = false)
    boolean autoApprove;

    @Column(name = "auto_approve_max_minor", nullable = false)
    long autoApproveMaxAmountMinor;

    @Column(name = "allow_refund", nullable = false)
    boolean allowRefund;

    @Column(name = "allow_exchange", nullable = false)
    boolean allowExchange;

    @Column(name = "allow_store_credit", nullable = false)
    boolean allowStoreCredit;

    @Column(name = "store_credit_bonus_minor", nullable = false)
    long storeCreditBonusMinor;

    /** Types de produits exclus, séparés par des virgules (ex. « Lingerie,Soldes »). */
    @Column(name = "excluded_product_types")
    String excludedProductTypes;

    protected PolicyEntity() {
    }
}

@Entity
@Table(name = "return_reason")
class ReasonEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    UUID tenantId;

    @Column(nullable = false)
    String code;

    @Column(name = "label_fr", nullable = false)
    String labelFr;

    @Column(name = "label_en", nullable = false)
    String labelEn;

    @Column(name = "label_ar", nullable = false)
    String labelAr;

    @Column(name = "requires_photo", nullable = false)
    boolean requiresPhoto;

    @Column(nullable = false)
    boolean active;

    @Column(nullable = false)
    int position;

    protected ReasonEntity() {
    }
}
