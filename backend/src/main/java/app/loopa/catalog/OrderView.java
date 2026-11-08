package app.loopa.catalog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Commande telle que connue de Loopa (copie locale de la plateforme e-commerce). */
public record OrderView(
        UUID id,
        String externalId,
        String name,
        String customerName,
        String customerEmail,
        String customerPhone,
        String currency,
        long totalMinor,
        String paymentGateway,
        Instant orderedAt,
        Instant fulfilledAt,
        List<Line> lines) {

    public record Line(
            UUID id,
            String externalLineId,
            String productId,
            String variantId,
            String title,
            String variantTitle,
            String sku,
            String productType,
            int quantity,
            long unitPriceMinor,
            String imageUrl) {
    }

    public java.util.Optional<Line> line(String externalLineId) {
        return lines.stream().filter(l -> l.externalLineId().equals(externalLineId)).findFirst();
    }
}
