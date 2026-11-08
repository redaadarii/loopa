package app.loopa.catalog.internal;

import app.loopa.catalog.CatalogApi;
import app.loopa.catalog.OrderUpsert;
import app.loopa.catalog.OrderView;
import app.loopa.shared.tenant.TenantContext;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByNameNormalized(String nameNormalized);

    Optional<OrderEntity> findByExternalId(String externalId);
}

@Service
class CatalogService implements CatalogApi {

    private final OrderRepository orders;
    private final Clock clock;

    CatalogService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> findForCustomer(String orderNumber, String contact) {
        if (orderNumber == null || contact == null || contact.isBlank()) {
            return Optional.empty();
        }
        return orders.findByNameNormalized(normalizeName(orderNumber))
                .filter(o -> contactMatches(o, contact))
                .map(CatalogService::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderView> findById(UUID orderId) {
        return orders.findById(orderId).map(CatalogService::toView);
    }

    @Override
    @Transactional
    public OrderView upsert(OrderUpsert in) {
        OrderEntity o = orders.findByExternalId(in.externalId()).orElseGet(() -> {
            OrderEntity e = new OrderEntity();
            e.id = UUID.randomUUID();
            e.tenantId = TenantContext.require();
            e.externalId = in.externalId();
            return e;
        });
        o.name = in.name();
        o.nameNormalized = normalizeName(in.name());
        o.customerName = in.customerName();
        o.customerEmail = in.customerEmail() == null ? null : in.customerEmail().toLowerCase(Locale.ROOT);
        o.customerPhone = digits(in.customerPhone());
        o.currency = in.currency();
        o.totalMinor = in.totalMinor();
        o.paymentGateway = in.paymentGateway();
        o.orderedAt = in.orderedAt();
        o.fulfilledAt = in.fulfilledAt();
        o.syncedAt = clock.instant();
        o.lines.clear();
        int position = 0;
        for (OrderUpsert.Line l : in.lines()) {
            OrderLineEntity le = new OrderLineEntity();
            le.id = UUID.randomUUID();
            le.tenantId = o.tenantId;
            le.order = o;
            le.position = position++;
            le.externalLineId = l.externalLineId();
            le.productId = l.productId();
            le.variantId = l.variantId();
            le.title = l.title();
            le.variantTitle = l.variantTitle();
            le.sku = l.sku();
            le.productType = l.productType();
            le.quantity = l.quantity();
            le.unitPriceMinor = l.unitPriceMinor();
            le.imageUrl = l.imageUrl();
            o.lines.add(le);
        }
        return toView(orders.save(o));
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return orders.count();
    }

    /** « #1042 », « 1042 » et « # 1042 » désignent la même commande. */
    static String normalizeName(String name) {
        return name.replaceAll("[^0-9A-Za-z]", "").toUpperCase(Locale.ROOT);
    }

    static String digits(String phone) {
        if (phone == null) {
            return null;
        }
        String d = phone.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    /** Email exact (insensible à la casse) ou 9 derniers chiffres du téléphone (0612… = +212612…). */
    static boolean contactMatches(OrderEntity o, String contact) {
        String c = contact.trim();
        if (c.contains("@")) {
            return o.customerEmail != null && o.customerEmail.equalsIgnoreCase(c);
        }
        String d = digits(c);
        if (d == null || d.length() < 9 || o.customerPhone == null || o.customerPhone.length() < 9) {
            return false;
        }
        return o.customerPhone.endsWith(d.substring(d.length() - 9));
    }

    static OrderView toView(OrderEntity o) {
        return new OrderView(o.id, o.externalId, o.name, o.customerName, o.customerEmail, o.customerPhone,
                o.currency, o.totalMinor, o.paymentGateway, o.orderedAt, o.fulfilledAt,
                o.lines.stream().map(l -> new OrderView.Line(l.id, l.externalLineId, l.productId, l.variantId,
                        l.title, l.variantTitle, l.sku, l.productType, l.quantity, l.unitPriceMinor, l.imageUrl))
                        .toList());
    }
}
