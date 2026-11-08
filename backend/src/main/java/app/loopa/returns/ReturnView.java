package app.loopa.returns;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Vue d'un retour partagée avec les autres modules et exposée par l'API. */
public record ReturnView(
        UUID id,
        String number,
        UUID orderId,
        String orderName,
        String customerName,
        String customerEmail,
        String locale,
        String status,
        String resolutionType,
        long totalMinor,
        long bonusMinor,
        long creditMinor,
        String currency,
        String customerNote,
        String rejectionReason,
        String carrier,
        String trackingNumber,
        String resolutionRef,
        String resolutionError,
        List<Line> lines,
        List<UUID> attachmentIds,
        List<HistoryEntry> history,
        Instant createdAt,
        Instant approvedAt,
        Instant updatedAt) {

    public record Line(UUID id, String orderLineId, String title, String variantTitle, int quantity,
                       long unitPriceMinor, String reasonCode, String exchangeVariantId) {
    }

    public record HistoryEntry(String type, String actor, String message, Instant occurredAt) {
    }
}
