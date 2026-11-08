package app.loopa.policy;

import app.loopa.catalog.OrderView;
import java.util.List;
import java.util.Map;

/** Façade publique du module policy (tenant courant). */
public interface PolicyApi {

    PolicyView policy();

    List<ReasonView> activeReasons();

    /**
     * @param alreadyReturned quantités déjà engagées dans des retours non annulés, par ligne de commande
     */
    Eligibility evaluate(OrderView order, Map<String, Integer> alreadyReturned);

    /** Vrai si le retour peut être approuvé sans intervention humaine. */
    boolean shouldAutoApprove(long totalMinor, List<String> reasonCodes);
}
