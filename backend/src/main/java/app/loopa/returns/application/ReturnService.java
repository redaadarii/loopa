package app.loopa.returns.application;

import app.loopa.returns.NewReturn;
import app.loopa.returns.ReturnEvents;
import app.loopa.returns.ReturnView;
import app.loopa.returns.ReturnsApi;
import app.loopa.returns.domain.ResolutionType;
import app.loopa.returns.domain.ReturnLine;
import app.loopa.returns.domain.ReturnRequest;
import app.loopa.returns.domain.ReturnStatus;
import app.loopa.shared.BusinessException;
import app.loopa.shared.Money;
import app.loopa.shared.tenant.TenantContext;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'usage du module returns. Chaque commande charge l'agrégat, appelle une méthode métier, sauvegarde,
 * trace l'historique puis publie les événements dans la même transaction (outbox Spring Modulith).
 */
@Service
public class ReturnService implements ReturnsApi {

    public static final String ACTOR_CUSTOMER = "CUSTOMER";
    public static final String ACTOR_SYSTEM = "SYSTEM";

    private final ReturnRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ReturnService(ReturnRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    /* ---------- ReturnsApi (portail, résolution) ---------- */

    @Override
    @Transactional
    public ReturnView submit(NewReturn cmd) {
        List<ReturnLine> lines = cmd.lines().stream()
                .map(l -> new ReturnLine(null, l.orderLineId(), l.title(), l.variantTitle(), l.quantity(),
                        Money.of(l.unitPriceMinor(), l.currency()), l.reasonCode(), l.exchangeVariantId()))
                .toList();
        var data = new ReturnRequest.NewReturnData(TenantContext.require(), cmd.orderId(), cmd.orderName(),
                cmd.customerName(), cmd.customerEmail(), cmd.locale(), ResolutionType.valueOf(cmd.resolutionType()),
                lines, cmd.attachmentIds(), cmd.customerNote(), cmd.bonusMinor());
        ReturnRequest request = ReturnRequest.submit(data, repository.nextNumber(), clock);
        ReturnRequest saved = saveAndPublish(request, ACTOR_CUSTOMER, "SUBMITTED", null);
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReturnView> find(UUID returnId) {
        return repository.findById(returnId).map(this::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnView> findByOrder(UUID orderId) {
        return repository.findByOrderId(orderId).stream().map(this::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> quantitiesInProgress(UUID orderId) {
        return repository.quantitiesInProgress(orderId);
    }

    @Override
    @Transactional
    public ReturnView approveAutomatically(UUID id) {
        return apply(id, r -> r.approve(true, clock), ACTOR_SYSTEM, "APPROVED", "Approbation automatique");
    }

    @Override
    @Transactional
    public ReturnView shipByCustomer(UUID id, String carrier, String trackingNumber) {
        return apply(id, r -> r.ship(carrier, trackingNumber, clock), ACTOR_CUSTOMER, "SHIPPED",
                (carrier == null ? "" : carrier + " · ") + trackingNumber);
    }

    @Override
    @Transactional
    public ReturnView cancelByCustomer(UUID id) {
        return apply(id, r -> r.cancel(clock), ACTOR_CUSTOMER, "CANCELLED", null);
    }

    @Override
    @Transactional
    public ReturnView markResolved(UUID id, String externalRef) {
        return apply(id, r -> r.resolve(externalRef, clock), ACTOR_SYSTEM, "RESOLVED", externalRef);
    }

    @Override
    @Transactional
    public void markResolutionFailed(UUID id, String error) {
        apply(id, r -> r.resolutionFailed(error, clock), ACTOR_SYSTEM, "RESOLUTION_FAILED", error);
    }

    /* ---------- Back-office ---------- */

    @Transactional
    public ReturnView approve(UUID id, String actor) {
        return apply(id, r -> r.approve(false, clock), actor, "APPROVED", null);
    }

    @Transactional
    public ReturnView reject(UUID id, String reason, String actor) {
        return apply(id, r -> r.reject(reason, clock), actor, "REJECTED", reason);
    }

    @Transactional
    public ReturnView receive(UUID id, String actor) {
        return apply(id, r -> r.receive(clock), actor, "RECEIVED", null);
    }

    /** Relance l'exécution d'une résolution en échec (module resolution, via événement). */
    @Transactional
    public ReturnView retryResolution(UUID id, String actor) {
        ReturnRequest request = load(id);
        if (request.status() != ReturnStatus.RECEIVED) {
            throw new BusinessException("NOT_RECEIVED", HttpStatus.CONFLICT,
                    "Le retour doit être reçu avant d'exécuter la résolution.");
        }
        repository.appendHistory(request.tenantId(), id, "RESOLUTION_RETRY", actor, null, clock.instant());
        events.publishEvent(new ReturnEvents.ResolutionRetryRequested(request.tenantId(), id, request.number(),
                clock.instant()));
        return toView(request);
    }

    @Transactional
    public ReturnView addNote(UUID id, String actor, String note) {
        ReturnRequest request = load(id);
        repository.appendHistory(request.tenantId(), id, "NOTE", actor, note, clock.instant());
        return toView(request);
    }

    @Transactional(readOnly = true)
    public List<ReturnView> search(ReturnStatus status, String query, UUID beforeId, int limit) {
        return repository.search(status, query, beforeId, Math.clamp(limit, 1, 100)).stream()
                .map(r -> toView(r, false)).toList();
    }

    @Transactional(readOnly = true)
    public Map<ReturnStatus, Long> countByStatus() {
        return repository.countByStatus();
    }

    @Transactional(readOnly = true)
    public ReturnView get(UUID id) {
        return toView(load(id));
    }

    /* ---------- interne ---------- */

    private ReturnRequest load(UUID id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound("Retour", id));
    }

    private ReturnView apply(UUID id, Consumer<ReturnRequest> action, String actor, String type, String message) {
        ReturnRequest request = load(id);
        action.accept(request);
        return toView(saveAndPublish(request, actor, type, message));
    }

    private ReturnRequest saveAndPublish(ReturnRequest request, String actor, String type, String message) {
        List<Object> pending = request.pullEvents();
        ReturnRequest saved = repository.save(request);
        repository.appendHistory(saved.tenantId(), saved.id(), type, actor, message, clock.instant());
        pending.forEach(events::publishEvent);
        return saved;
    }

    private ReturnView toView(ReturnRequest r) {
        return toView(r, true);
    }

    private ReturnView toView(ReturnRequest r, boolean withHistory) {
        Money total = r.total();
        return new ReturnView(r.id(), r.number(), r.orderId(), r.orderName(), r.customerName(), r.customerEmail(),
                r.locale(), r.status().name(), r.resolutionType().name(), total.amountMinor(), r.bonusMinor(),
                r.creditAmount().amountMinor(), total.currency().getCurrencyCode(), r.customerNote(),
                r.rejectionReason(), r.carrier(), r.trackingNumber(), r.resolutionRef(), r.resolutionError(),
                r.lines().stream().map(l -> new ReturnView.Line(l.id(), l.orderLineId(), l.title(), l.variantTitle(),
                        l.quantity(), l.unitPrice().amountMinor(), l.reasonCode(), l.exchangeVariantId())).toList(),
                r.attachmentIds(),
                withHistory ? repository.history(r.id()) : List.of(),
                r.createdAt(), r.approvedAt(), r.updatedAt());
    }
}
