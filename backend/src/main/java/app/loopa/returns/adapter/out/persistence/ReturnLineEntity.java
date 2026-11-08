package app.loopa.returns.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "return_line")
class ReturnLineEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id")
    ReturnRequestEntity request;

    @Column(nullable = false)
    int position;

    @Column(name = "order_line_id", nullable = false)
    String orderLineId;

    String title;

    @Column(name = "variant_title")
    String variantTitle;

    @Column(nullable = false)
    int quantity;

    @Column(name = "unit_price_minor", nullable = false)
    long unitPriceMinor;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(name = "reason_code", nullable = false)
    String reasonCode;

    @Column(name = "exchange_variant_id")
    String exchangeVariantId;

    protected ReturnLineEntity() {
    }
}
