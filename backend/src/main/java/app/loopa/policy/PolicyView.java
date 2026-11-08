package app.loopa.policy;

import java.util.List;

/** Politique de retour d'une boutique. */
public record PolicyView(
        int windowDays,
        boolean autoApprove,
        long autoApproveMaxAmountMinor,
        boolean allowRefund,
        boolean allowExchange,
        boolean allowStoreCredit,
        long storeCreditBonusMinor,
        List<String> excludedProductTypes) {

    public List<String> allowedResolutions() {
        var list = new java.util.ArrayList<String>();
        if (allowStoreCredit) {
            list.add("STORE_CREDIT");
        }
        if (allowExchange) {
            list.add("EXCHANGE");
        }
        if (allowRefund) {
            list.add("REFUND");
        }
        return List.copyOf(list);
    }
}
