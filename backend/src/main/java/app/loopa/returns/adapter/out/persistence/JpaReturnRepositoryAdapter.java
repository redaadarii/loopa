package app.loopa.returns.adapter.out.persistence;

import app.loopa.returns.ReturnView;
import app.loopa.returns.application.ReturnRepository;
import app.loopa.returns.domain.ReturnLine;
import app.loopa.returns.domain.ReturnRequest;
import app.loopa.returns.domain.ReturnStatus;
import app.loopa.shared.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

/**
 * Adaptateur de sortie : traduit l'agrégat {@link ReturnRequest} vers les entités JPA et inversement.
 */
@Repository
class JpaReturnRepositoryAdapter implements ReturnRepository {

    private final SpringDataReturnRepository jpa;
    private final SpringDataReturnHistoryRepository historyJpa;
    private final Clock clock;

    JpaReturnRepositoryAdapter(SpringDataReturnRepository jpa, SpringDataReturnHistoryRepository historyJpa,
                               Clock clock) {
        this.jpa = jpa;
        this.historyJpa = historyJpa;
        this.clock = clock;
    }

    @Override
    public ReturnRequest save(ReturnRequest domain) {
        ReturnRequestEntity entity = jpa.findById(domain.id()).orElse(null);
        if (entity == null) {
            entity = newEntity(domain);
        } else if (!Objects.equals(entity.version, domain.version())) {
            throw new ObjectOptimisticLockingFailureException(ReturnRequestEntity.class, domain.id());
        }
        entity.status = domain.status();
        entity.rejectionReason = domain.rejectionReason();
        entity.carrier = domain.carrier();
        entity.trackingNumber = domain.trackingNumber();
        entity.resolutionRef = domain.resolutionRef();
        entity.resolutionError = domain.resolutionError();
        entity.approvedAt = domain.approvedAt();
        entity.updatedAt = domain.updatedAt();
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public Optional<ReturnRequest> findById(UUID id) {
        return jpa.findById(id).map(JpaReturnRepositoryAdapter::toDomain);
    }

    @Override
    public List<ReturnRequest> findByOrderId(UUID orderId) {
        return jpa.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(JpaReturnRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<ReturnRequest> search(ReturnStatus status, String query, UUID beforeId, int limit) {
        String pattern = query == null || query.isBlank()
                ? "%"
                : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        return jpa.search(status, pattern, beforeId, Limit.of(limit)).stream()
                .map(JpaReturnRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Map<ReturnStatus, Long> countByStatus() {
        Map<ReturnStatus, Long> counts = new EnumMap<>(ReturnStatus.class);
        for (ReturnStatus s : ReturnStatus.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : jpa.countGroupedByStatus()) {
            counts.put((ReturnStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Override
    public Map<String, Integer> quantitiesInProgress(UUID orderId) {
        Map<String, Integer> result = new HashMap<>();
        for (Object[] row : jpa.quantitiesByLine(orderId, List.of(ReturnStatus.CANCELLED, ReturnStatus.REJECTED))) {
            result.put((String) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    @Override
    public String nextNumber() {
        return "RMA-%d-%06d".formatted(Year.now(clock).getValue(), jpa.nextNumberValue());
    }

    @Override
    public void appendHistory(UUID tenantId, UUID returnId, String type, String actor, String message, Instant at) {
        historyJpa.save(new ReturnHistoryEntity(tenantId, returnId, type, actor, message, at));
    }

    @Override
    public List<ReturnView.HistoryEntry> history(UUID returnId) {
        return historyJpa.findByReturnRequestIdOrderByOccurredAt(returnId).stream()
                .map(h -> new ReturnView.HistoryEntry(h.type, h.actor, h.message, h.occurredAt))
                .toList();
    }

    private static ReturnRequestEntity newEntity(ReturnRequest d) {
        ReturnRequestEntity e = new ReturnRequestEntity();
        e.id = d.id();
        e.tenantId = d.tenantId();
        e.number = d.number();
        e.orderId = d.orderId();
        e.orderName = d.orderName();
        e.customerName = d.customerName();
        e.customerEmail = d.customerEmail();
        e.locale = d.locale();
        e.resolutionType = d.resolutionType();
        e.customerNote = d.customerNote();
        e.bonusMinor = d.bonusMinor();
        e.createdAt = d.createdAt();
        e.attachmentIds = new ArrayList<>(d.attachmentIds());
        int position = 0;
        for (ReturnLine l : d.lines()) {
            ReturnLineEntity le = new ReturnLineEntity();
            le.id = l.id();
            le.tenantId = d.tenantId();
            le.request = e;
            le.position = position++;
            le.orderLineId = l.orderLineId();
            le.title = l.title();
            le.variantTitle = l.variantTitle();
            le.quantity = l.quantity();
            le.unitPriceMinor = l.unitPrice().amountMinor();
            le.currency = l.unitPrice().currency().getCurrencyCode();
            le.reasonCode = l.reasonCode();
            le.exchangeVariantId = l.exchangeVariantId();
            e.lines.add(le);
        }
        return e;
    }

    private static ReturnRequest toDomain(ReturnRequestEntity e) {
        List<ReturnLine> lines = e.lines.stream()
                .map(l -> new ReturnLine(l.id, l.orderLineId, l.title, l.variantTitle, l.quantity,
                        Money.of(l.unitPriceMinor, l.currency), l.reasonCode, l.exchangeVariantId))
                .toList();
        return ReturnRequest.rehydrate()
                .id(e.id).tenantId(e.tenantId).number(e.number).orderId(e.orderId).orderName(e.orderName)
                .customerName(e.customerName).customerEmail(e.customerEmail).locale(e.locale)
                .resolutionType(e.resolutionType).lines(lines).attachmentIds(List.copyOf(e.attachmentIds))
                .customerNote(e.customerNote).bonusMinor(e.bonusMinor).status(e.status)
                .rejectionReason(e.rejectionReason).carrier(e.carrier).trackingNumber(e.trackingNumber)
                .resolutionRef(e.resolutionRef).resolutionError(e.resolutionError)
                .approvedAt(e.approvedAt).createdAt(e.createdAt).updatedAt(e.updatedAt)
                .version(e.version)
                .build();
    }
}
