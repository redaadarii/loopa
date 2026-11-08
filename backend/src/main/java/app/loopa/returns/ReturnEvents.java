package app.loopa.returns;

import java.time.Instant;
import java.util.UUID;

/**
 * Événements publics du module returns. Les autres modules (resolution, notification, billing, analytics)
 * les écoutent après commit ; ils sont persistés dans l'outbox Spring Modulith et rejoués en cas d'échec.
 */
public final class ReturnEvents {

    private ReturnEvents() {
    }

    public record ReturnSubmitted(UUID tenantId, UUID returnId, String number, Instant occurredAt) {
    }

    public record ReturnApproved(UUID tenantId, UUID returnId, String number, boolean automatic, Instant occurredAt) {
    }

    public record ReturnRejected(UUID tenantId, UUID returnId, String number, String reason, Instant occurredAt) {
    }

    public record ReturnShipped(UUID tenantId, UUID returnId, String number, String trackingNumber,
                                Instant occurredAt) {
    }

    public record ReturnReceived(UUID tenantId, UUID returnId, String number, Instant occurredAt) {
    }

    public record ReturnResolved(UUID tenantId, UUID returnId, String number, Instant occurredAt) {
    }

    /** Le marchand relance l'exécution d'une résolution qui a échoué. */
    public record ResolutionRetryRequested(UUID tenantId, UUID returnId, String number, Instant occurredAt) {
    }

    public record ReturnCancelled(UUID tenantId, UUID returnId, String number, Instant occurredAt) {
    }
}
