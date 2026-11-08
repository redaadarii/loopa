package app.loopa.returns;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Façade publique du module returns, utilisée par le portail, la résolution et les notifications. */
public interface ReturnsApi {

    ReturnView submit(NewReturn request);

    Optional<ReturnView> find(UUID returnId);

    List<ReturnView> findByOrder(UUID orderId);

    /** Quantités déjà engagées dans des retours non annulés ni rejetés, par ligne de commande. */
    Map<String, Integer> quantitiesInProgress(UUID orderId);

    ReturnView approveAutomatically(UUID returnId);

    ReturnView shipByCustomer(UUID returnId, String carrier, String trackingNumber);

    ReturnView cancelByCustomer(UUID returnId);

    ReturnView markResolved(UUID returnId, String externalRef);

    void markResolutionFailed(UUID returnId, String error);
}
