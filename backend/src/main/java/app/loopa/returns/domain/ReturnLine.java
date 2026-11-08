package app.loopa.returns.domain;

import app.loopa.shared.Money;
import java.util.Objects;
import java.util.UUID;

/**
 * Un article retourné. Valeur immuable de l'agrégat {@link ReturnRequest}.
 * Titre et prix sont copiés depuis la commande au moment de la demande (jamais fournis par le client).
 *
 * @param exchangeVariantId variante souhaitée en échange (null si pas d'échange)
 */
public record ReturnLine(
        UUID id,
        String orderLineId,
        String title,
        String variantTitle,
        int quantity,
        Money unitPrice,
        String reasonCode,
        String exchangeVariantId) {

    public ReturnLine {
        Objects.requireNonNull(orderLineId, "orderLineId");
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(reasonCode, "reasonCode");
        if (quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être positive");
        }
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public Money total() {
        return unitPrice.times(quantity);
    }
}
