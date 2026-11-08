package app.loopa.returns.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'un retour. Les transitions autorisées sont déclarées ici et nulle part ailleurs.
 */
public enum ReturnStatus {
    SUBMITTED,
    APPROVED,
    REJECTED,
    CANCELLED,
    IN_TRANSIT,
    RECEIVED,
    RESOLVED;

    public Set<ReturnStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(APPROVED, REJECTED, CANCELLED);
            case APPROVED -> EnumSet.of(IN_TRANSIT, RECEIVED, CANCELLED);
            case IN_TRANSIT -> EnumSet.of(RECEIVED);
            case RECEIVED -> EnumSet.of(RESOLVED, REJECTED);
            case REJECTED, CANCELLED, RESOLVED -> EnumSet.noneOf(ReturnStatus.class);
        };
    }

    public boolean canMoveTo(ReturnStatus target) {
        return allowedNext().contains(target);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
