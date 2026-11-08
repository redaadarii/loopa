package app.loopa.portal.internal;

import app.loopa.billing.BillingApi;
import app.loopa.catalog.CatalogApi;
import app.loopa.catalog.OrderView;
import app.loopa.policy.Eligibility;
import app.loopa.policy.PolicyApi;
import app.loopa.policy.PolicyView;
import app.loopa.policy.ReasonView;
import app.loopa.returns.NewReturn;
import app.loopa.returns.ReturnView;
import app.loopa.returns.ReturnsApi;
import app.loopa.shared.BusinessException;
import app.loopa.storage.StorageApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parcours de retour du client final. Les titres et prix viennent toujours de la commande enregistrée,
 * jamais de la requête : le client ne choisit que les lignes, les quantités, les motifs et la solution.
 */
@Service
class PortalReturnService {

    private final CatalogApi catalog;
    private final PolicyApi policy;
    private final ReturnsApi returns;
    private final BillingApi billing;
    private final StorageApi storage;

    PortalReturnService(CatalogApi catalog, PolicyApi policy, ReturnsApi returns, BillingApi billing,
                        StorageApi storage) {
        this.catalog = catalog;
        this.policy = policy;
        this.returns = returns;
        this.billing = billing;
        this.storage = storage;
    }

    record OrderLineDto(String orderLineId, String title, String variantTitle, String sku, String productType,
                        int quantity, int returnableQuantity, long unitPriceMinor, String imageUrl,
                        boolean eligible, String ineligibleReason) {
    }

    record ExistingReturnDto(UUID id, String number, String status, Instant createdAt) {
    }

    record OrderDto(UUID orderId, String orderName, String customerName, String currency, Instant orderedAt,
                    Instant fulfilledAt, Instant eligibleUntil, boolean windowOpen, String paymentGateway,
                    List<OrderLineDto> lines, List<ReasonView> reasons, List<String> resolutions,
                    long storeCreditBonusMinor, List<ExistingReturnDto> existingReturns) {
    }

    record LineRequest(String orderLineId, int quantity, String reasonCode, String exchangePreference) {
    }

    record SubmitRequest(String resolutionType, List<LineRequest> lines, List<UUID> attachmentIds,
                         String customerNote, String locale) {
    }

    @Transactional
    OrderDto order(UUID orderId) {
        OrderView order = loadOrder(orderId);
        PolicyView p = policy.policy();
        Eligibility eligibility = policy.evaluate(order, returns.quantitiesInProgress(orderId));
        List<OrderLineDto> lines = new ArrayList<>();
        for (OrderView.Line l : order.lines()) {
            Eligibility.LineEligibility e = eligibility.lines().stream()
                    .filter(x -> x.externalLineId().equals(l.externalLineId())).findFirst().orElseThrow();
            lines.add(new OrderLineDto(l.externalLineId(), l.title(), l.variantTitle(), l.sku(), l.productType(),
                    l.quantity(), e.returnableQuantity(), l.unitPriceMinor(), l.imageUrl(), e.eligible(), e.reason()));
        }
        List<ExistingReturnDto> existing = returns.findByOrder(orderId).stream()
                .map(r -> new ExistingReturnDto(r.id(), r.number(), r.status(), r.createdAt())).toList();
        return new OrderDto(order.id(), order.name(), order.customerName(), order.currency(), order.orderedAt(),
                order.fulfilledAt(), eligibility.eligibleUntil(), eligibility.windowOpen(), order.paymentGateway(),
                lines, policy.activeReasons(), p.allowedResolutions(), p.storeCreditBonusMinor(), existing);
    }

    @Transactional
    ReturnView submit(UUID orderId, SubmitRequest req) {
        billing.assertCanCreateReturn();
        OrderView order = loadOrder(orderId);
        PolicyView p = policy.policy();
        if (req.resolutionType() == null || !p.allowedResolutions().contains(req.resolutionType())) {
            throw invalid("Cette solution n'est pas proposée par la boutique.");
        }
        if (req.lines() == null || req.lines().isEmpty()) {
            throw invalid("Choisissez au moins un article.");
        }
        Eligibility eligibility = policy.evaluate(order, returns.quantitiesInProgress(orderId));
        List<ReasonView> reasons = policy.activeReasons();
        List<NewReturn.Line> lines = new ArrayList<>();
        boolean photoRequired = false;
        var seen = new HashSet<String>();
        for (LineRequest lr : req.lines()) {
            if (!seen.add(lr.orderLineId())) {
                throw invalid("Un article apparaît deux fois.");
            }
            OrderView.Line line = order.line(lr.orderLineId()).orElseThrow(() -> invalid("Article inconnu."));
            Eligibility.LineEligibility e = eligibility.lines().stream()
                    .filter(x -> x.externalLineId().equals(lr.orderLineId())).findFirst().orElseThrow();
            if (!e.eligible() || lr.quantity() < 1 || lr.quantity() > e.returnableQuantity()) {
                throw invalid("« " + line.title() + " » ne peut pas être retourné dans cette quantité.");
            }
            ReasonView reason = reasons.stream().filter(r -> r.code().equals(lr.reasonCode())).findFirst()
                    .orElseThrow(() -> invalid("Motif inconnu."));
            photoRequired |= reason.requiresPhoto();
            String exchange = "EXCHANGE".equals(req.resolutionType()) ? blankToNull(lr.exchangePreference()) : null;
            lines.add(new NewReturn.Line(line.externalLineId(), line.title(), line.variantTitle(), lr.quantity(),
                    line.unitPriceMinor(), order.currency(), reason.code(), exchange));
        }
        List<UUID> attachments = req.attachmentIds() == null ? List.of() : List.copyOf(req.attachmentIds());
        if (photoRequired && attachments.isEmpty()) {
            throw invalid("Ajoutez au moins une photo pour ce motif.");
        }
        for (UUID id : attachments) {
            storage.find(id).orElseThrow(() -> invalid("Photo introuvable."));
        }
        ReturnView created = returns.submit(new NewReturn(order.id(), order.name(), order.customerName(),
                order.customerEmail(), normalizeLocale(req.locale()), req.resolutionType(), lines, attachments,
                blankToNull(req.customerNote()), p.storeCreditBonusMinor()));

        List<String> codes = lines.stream().map(NewReturn.Line::reasonCode).toList();
        if (policy.shouldAutoApprove(created.totalMinor(), codes)) {
            return returns.approveAutomatically(created.id());
        }
        return created;
    }

    @Transactional(readOnly = true)
    ReturnView get(UUID orderId, UUID returnId) {
        ReturnView r = returns.find(returnId).orElseThrow(() -> BusinessException.notFound("Retour", returnId));
        if (!r.orderId().equals(orderId)) {
            // Un jeton portail ne donne accès qu'aux retours de SA commande.
            throw BusinessException.notFound("Retour", returnId);
        }
        return r;
    }

    @Transactional
    ReturnView addTracking(UUID orderId, UUID returnId, String carrier, String trackingNumber) {
        get(orderId, returnId);
        return returns.shipByCustomer(returnId, blankToNull(carrier), trackingNumber);
    }

    @Transactional
    ReturnView cancel(UUID orderId, UUID returnId) {
        get(orderId, returnId);
        return returns.cancelByCustomer(returnId);
    }

    private OrderView loadOrder(UUID orderId) {
        return catalog.findById(orderId).orElseThrow(() -> BusinessException.notFound("Commande", orderId));
    }

    private static BusinessException invalid(String message) {
        return new BusinessException("INVALID_RETURN", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String normalizeLocale(String locale) {
        return locale != null && Map.of("fr", 1, "en", 1, "ar", 1).containsKey(locale) ? locale : "fr";
    }
}
