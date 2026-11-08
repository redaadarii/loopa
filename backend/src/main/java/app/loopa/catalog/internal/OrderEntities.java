package app.loopa.catalog.internal;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "order_snapshot")
class OrderEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    UUID tenantId;

    @Column(name = "external_id", nullable = false)
    String externalId;

    @Column(nullable = false)
    String name;

    @Column(name = "name_normalized", nullable = false)
    String nameNormalized;

    @Column(name = "customer_name")
    String customerName;

    @Column(name = "customer_email")
    String customerEmail;

    @Column(name = "customer_phone")
    String customerPhone;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(name = "total_minor", nullable = false)
    long totalMinor;

    @Column(name = "payment_gateway")
    String paymentGateway;

    @Column(name = "ordered_at", nullable = false)
    Instant orderedAt;

    @Column(name = "fulfilled_at")
    Instant fulfilledAt;

    @Column(name = "synced_at", nullable = false)
    Instant syncedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    @BatchSize(size = 50)
    List<OrderLineEntity> lines = new ArrayList<>();

    protected OrderEntity() {
    }
}

@Entity
@Table(name = "order_line_snapshot")
class OrderLineEntity {

    @Id
    UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    OrderEntity order;

    @Column(nullable = false)
    int position;

    @Column(name = "external_line_id", nullable = false)
    String externalLineId;

    @Column(name = "product_id")
    String productId;

    @Column(name = "variant_id")
    String variantId;

    @Column(nullable = false)
    String title;

    @Column(name = "variant_title")
    String variantTitle;

    String sku;

    @Column(name = "product_type")
    String productType;

    @Column(nullable = false)
    int quantity;

    @Column(name = "unit_price_minor", nullable = false)
    long unitPriceMinor;

    @Column(name = "image_url")
    String imageUrl;

    protected OrderLineEntity() {
    }
}
