package app.loopa.returns;

import java.util.List;
import java.util.UUID;

/** Demande de création d'un retour, déjà validée (éligibilité, prix issus de la commande). */
public record NewReturn(
        UUID orderId,
        String orderName,
        String customerName,
        String customerEmail,
        String locale,
        String resolutionType,
        List<Line> lines,
        List<UUID> attachmentIds,
        String customerNote,
        long bonusMinor) {

    public record Line(String orderLineId, String title, String variantTitle, int quantity, long unitPriceMinor,
                       String currency, String reasonCode, String exchangeVariantId) {
    }
}
