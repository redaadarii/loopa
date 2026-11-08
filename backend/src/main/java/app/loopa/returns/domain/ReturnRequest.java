package app.loopa.returns.domain;

import app.loopa.returns.ReturnEvents.ReturnApproved;
import app.loopa.returns.ReturnEvents.ReturnCancelled;
import app.loopa.returns.ReturnEvents.ReturnReceived;
import app.loopa.returns.ReturnEvents.ReturnRejected;
import app.loopa.returns.ReturnEvents.ReturnResolved;
import app.loopa.returns.ReturnEvents.ReturnShipped;
import app.loopa.returns.ReturnEvents.ReturnSubmitted;
import app.loopa.shared.Money;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat central : une demande de retour sur une commande.
 * <p>
 * Sans dépendance à JPA ni au web : testable en Java pur. Chaque changement d'état passe par une méthode
 * métier qui vérifie la transition et enregistre un événement de domaine, publié par le service applicatif.
 */
public class ReturnRequest {

    private final UUID id;
    private final UUID tenantId;
    private final String number;
    private final UUID orderId;
    private final String orderName;
    private final String customerName;
    private final String customerEmail;
    private final String locale;
    private final ResolutionType resolutionType;
    private final List<ReturnLine> lines;
    private final List<UUID> attachmentIds;
    private final String customerNote;
    private final long bonusMinor;
    private final Instant createdAt;
    private final Long version;
    private ReturnStatus status;
    private String rejectionReason;
    private String carrier;
    private String trackingNumber;
    private String resolutionRef;
    private String resolutionError;
    private Instant approvedAt;
    private Instant updatedAt;

    private final List<Object> pendingEvents = new ArrayList<>();

    private ReturnRequest(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.tenantId = Objects.requireNonNull(b.tenantId, "tenantId");
        this.number = Objects.requireNonNull(b.number, "number");
        this.orderId = Objects.requireNonNull(b.orderId, "orderId");
        this.orderName = b.orderName;
        this.customerName = b.customerName;
        this.customerEmail = b.customerEmail;
        this.locale = b.locale == null ? "fr" : b.locale;
        this.resolutionType = Objects.requireNonNull(b.resolutionType, "resolutionType");
        this.lines = List.copyOf(b.lines);
        this.attachmentIds = List.copyOf(b.attachmentIds);
        this.customerNote = b.customerNote;
        this.bonusMinor = b.bonusMinor;
        this.createdAt = b.createdAt;
        this.version = b.version;
        this.status = Objects.requireNonNull(b.status, "status");
        this.rejectionReason = b.rejectionReason;
        this.carrier = b.carrier;
        this.trackingNumber = b.trackingNumber;
        this.resolutionRef = b.resolutionRef;
        this.resolutionError = b.resolutionError;
        this.approvedAt = b.approvedAt;
        this.updatedAt = b.updatedAt;
    }

    /** Crée une nouvelle demande au statut SUBMITTED. Le bonus ne s'applique qu'à un avoir. */
    public static ReturnRequest submit(NewReturnData data, String number, Clock clock) {
        if (data.lines() == null || data.lines().isEmpty()) {
            throw new IllegalArgumentException("Un retour contient au moins un article");
        }
        Instant now = clock.instant();
        ReturnRequest r = new Builder()
                .id(UUID.randomUUID()).tenantId(data.tenantId()).number(number)
                .orderId(data.orderId()).orderName(data.orderName())
                .customerName(data.customerName()).customerEmail(data.customerEmail()).locale(data.locale())
                .resolutionType(data.resolutionType()).lines(data.lines()).attachmentIds(data.attachmentIds())
                .customerNote(data.customerNote())
                .bonusMinor(data.resolutionType() == ResolutionType.STORE_CREDIT ? data.bonusMinor() : 0)
                .status(ReturnStatus.SUBMITTED).createdAt(now).updatedAt(now)
                .build();
        r.pendingEvents.add(new ReturnSubmitted(r.tenantId, r.id, number, now));
        return r;
    }

    /** Données de création, déjà validées par la politique de retour. */
    public record NewReturnData(UUID tenantId, UUID orderId, String orderName, String customerName,
                                String customerEmail, String locale, ResolutionType resolutionType,
                                List<ReturnLine> lines, List<UUID> attachmentIds, String customerNote,
                                long bonusMinor) {
    }

    public void approve(boolean automatic, Clock clock) {
        moveTo(ReturnStatus.APPROVED, clock);
        this.approvedAt = updatedAt;
        pendingEvents.add(new ReturnApproved(tenantId, id, number, automatic, updatedAt));
    }

    public void reject(String reason, Clock clock) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Un motif de rejet est obligatoire");
        }
        moveTo(ReturnStatus.REJECTED, clock);
        this.rejectionReason = reason.trim();
        pendingEvents.add(new ReturnRejected(tenantId, id, number, this.rejectionReason, updatedAt));
    }

    public void ship(String carrier, String trackingNumber, Clock clock) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("Le numéro de suivi est obligatoire");
        }
        moveTo(ReturnStatus.IN_TRANSIT, clock);
        this.carrier = carrier;
        this.trackingNumber = trackingNumber.trim();
        pendingEvents.add(new ReturnShipped(tenantId, id, number, this.trackingNumber, updatedAt));
    }

    public void receive(Clock clock) {
        moveTo(ReturnStatus.RECEIVED, clock);
        pendingEvents.add(new ReturnReceived(tenantId, id, number, updatedAt));
    }

    /** La résolution (remboursement, échange, avoir) a été exécutée sur la plateforme. */
    public void resolve(String externalRef, Clock clock) {
        moveTo(ReturnStatus.RESOLVED, clock);
        this.resolutionRef = externalRef;
        this.resolutionError = null;
        pendingEvents.add(new ReturnResolved(tenantId, id, number, updatedAt));
    }

    /** L'exécution a échoué : le retour reste RECEIVED, l'erreur est visible et l'action peut être relancée. */
    public void resolutionFailed(String error, Clock clock) {
        if (status != ReturnStatus.RECEIVED) {
            throw new InvalidTransitionException(number, status, ReturnStatus.RESOLVED);
        }
        this.resolutionError = error;
        this.updatedAt = clock.instant();
    }

    public void cancel(Clock clock) {
        moveTo(ReturnStatus.CANCELLED, clock);
        pendingEvents.add(new ReturnCancelled(tenantId, id, number, updatedAt));
    }

    private void moveTo(ReturnStatus target, Clock clock) {
        if (!status.canMoveTo(target)) {
            throw new InvalidTransitionException(number, status, target);
        }
        this.status = target;
        this.updatedAt = clock.instant();
    }

    /** Valeur des articles retournés. */
    public Money total() {
        return lines.stream().map(ReturnLine::total).reduce(Money::plus).orElseThrow();
    }

    /** Montant rendu au client : valeur des articles + bonus éventuel. */
    public Money creditAmount() {
        Money total = total();
        return new Money(total.amountMinor() + bonusMinor, total.currency());
    }

    /** Événements produits depuis le chargement ; vidés après publication. */
    public List<Object> pullEvents() {
        List<Object> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public String number() { return number; }
    public UUID orderId() { return orderId; }
    public String orderName() { return orderName; }
    public String customerName() { return customerName; }
    public String customerEmail() { return customerEmail; }
    public String locale() { return locale; }
    public ResolutionType resolutionType() { return resolutionType; }
    public List<ReturnLine> lines() { return lines; }
    public List<UUID> attachmentIds() { return attachmentIds; }
    public String customerNote() { return customerNote; }
    public long bonusMinor() { return bonusMinor; }
    public ReturnStatus status() { return status; }
    public String rejectionReason() { return rejectionReason; }
    public String carrier() { return carrier; }
    public String trackingNumber() { return trackingNumber; }
    public String resolutionRef() { return resolutionRef; }
    public String resolutionError() { return resolutionError; }
    public Instant approvedAt() { return approvedAt; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Long version() { return version; }

    /** Reconstitution depuis la persistance (utilisé uniquement par l'adaptateur JPA). */
    public static Builder rehydrate() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private UUID tenantId;
        private String number;
        private UUID orderId;
        private String orderName;
        private String customerName;
        private String customerEmail;
        private String locale;
        private ResolutionType resolutionType;
        private List<ReturnLine> lines = List.of();
        private List<UUID> attachmentIds = List.of();
        private String customerNote;
        private long bonusMinor;
        private ReturnStatus status;
        private String rejectionReason;
        private String carrier;
        private String trackingNumber;
        private String resolutionRef;
        private String resolutionError;
        private Instant approvedAt;
        private Instant createdAt;
        private Instant updatedAt;
        private Long version;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder tenantId(UUID v) { this.tenantId = v; return this; }
        public Builder number(String v) { this.number = v; return this; }
        public Builder orderId(UUID v) { this.orderId = v; return this; }
        public Builder orderName(String v) { this.orderName = v; return this; }
        public Builder customerName(String v) { this.customerName = v; return this; }
        public Builder customerEmail(String v) { this.customerEmail = v; return this; }
        public Builder locale(String v) { this.locale = v; return this; }
        public Builder resolutionType(ResolutionType v) { this.resolutionType = v; return this; }
        public Builder lines(List<ReturnLine> v) { this.lines = v == null ? List.of() : v; return this; }
        public Builder attachmentIds(List<UUID> v) { this.attachmentIds = v == null ? List.of() : v; return this; }
        public Builder customerNote(String v) { this.customerNote = v; return this; }
        public Builder bonusMinor(long v) { this.bonusMinor = v; return this; }
        public Builder status(ReturnStatus v) { this.status = v; return this; }
        public Builder rejectionReason(String v) { this.rejectionReason = v; return this; }
        public Builder carrier(String v) { this.carrier = v; return this; }
        public Builder trackingNumber(String v) { this.trackingNumber = v; return this; }
        public Builder resolutionRef(String v) { this.resolutionRef = v; return this; }
        public Builder resolutionError(String v) { this.resolutionError = v; return this; }
        public Builder approvedAt(Instant v) { this.approvedAt = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }
        public Builder version(Long v) { this.version = v; return this; }

        public ReturnRequest build() {
            return new ReturnRequest(this);
        }
    }
}
