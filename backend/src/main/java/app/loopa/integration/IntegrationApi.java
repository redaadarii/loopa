package app.loopa.integration;

import app.loopa.returns.ReturnView;
import java.util.UUID;

/** Façade publique du module integration. */
public interface IntegrationApi {

    /**
     * Exécute la résolution d'un retour sur Shopify et renvoie la référence créée
     * (remboursement, commande d'échange ou carte cadeau).
     */
    String executeShopifyResolution(UUID shopId, ReturnView returnView);
}
