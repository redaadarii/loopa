package app.loopa.policy;

import java.time.Instant;
import java.util.List;

/** Résultat de l'évaluation d'une commande par la politique de retour. */
public record Eligibility(Instant eligibleUntil, boolean windowOpen, List<LineEligibility> lines) {

    public record LineEligibility(String externalLineId, int returnableQuantity, boolean eligible, String reason) {
    }
}
