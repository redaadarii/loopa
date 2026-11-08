package app.loopa.catalog;

import java.time.Instant;
import java.util.List;

/** Données reçues de la plateforme (webhook ou synchro) pour créer ou mettre à jour une commande. */
public record OrderUpsert(
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

    public record Line(String externalLineId, String productId, String variantId, String title, String variantTitle,
                       String sku, String productType, int quantity, long unitPriceMinor, String imageUrl) {
    }
}
