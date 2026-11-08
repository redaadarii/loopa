package app.loopa.returns.application;

import app.loopa.returns.ReturnView;
import app.loopa.returns.domain.ReturnRequest;
import app.loopa.returns.domain.ReturnStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Ports de sortie du module returns. Implémentés par l'adaptateur JPA.
 * Le filtrage par tenant est automatique (Hibernate {@code @TenantId}).
 */
public interface ReturnRepository {

    ReturnRequest save(ReturnRequest request);

    Optional<ReturnRequest> findById(UUID id);

    List<ReturnRequest> findByOrderId(UUID orderId);

    /** Pagination par curseur : retours plus anciens que {@code beforeId}, du plus récent au plus ancien. */
    List<ReturnRequest> search(ReturnStatus status, String query, UUID beforeId, int limit);

    Map<ReturnStatus, Long> countByStatus();

    /** Quantités engagées par ligne de commande, hors retours annulés ou rejetés. */
    Map<String, Integer> quantitiesInProgress(UUID orderId);

    /** Numéro lisible et unique, ex. RMA-2026-000123. */
    String nextNumber();

    void appendHistory(UUID tenantId, UUID returnId, String type, String actor, String message, Instant at);

    List<ReturnView.HistoryEntry> history(UUID returnId);
}
