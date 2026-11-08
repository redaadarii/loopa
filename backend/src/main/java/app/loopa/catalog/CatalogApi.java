package app.loopa.catalog;

import java.util.Optional;
import java.util.UUID;

/** Façade publique du module catalog. Toutes les méthodes s'appliquent au tenant courant. */
public interface CatalogApi {

    /**
     * Retrouve une commande à partir de son numéro (« #1042 » ou « 1042 ») et de l'email ou du téléphone
     * du client. Renvoie vide si l'un des deux ne correspond pas, sans dire lequel.
     */
    Optional<OrderView> findForCustomer(String orderNumber, String contact);

    Optional<OrderView> findById(UUID orderId);

    OrderView upsert(OrderUpsert order);

    long count();
}
