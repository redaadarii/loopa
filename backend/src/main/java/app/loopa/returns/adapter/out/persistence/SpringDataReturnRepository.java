package app.loopa.returns.adapter.out.persistence;

import app.loopa.returns.domain.ReturnStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataReturnRepository extends JpaRepository<ReturnRequestEntity, UUID> {

    @Query("""
            select r from ReturnRequestEntity r
            where (:status is null or r.status = :status)
              and (lower(r.number) like :q or lower(coalesce(r.customerEmail, '')) like :q
                   or lower(coalesce(r.customerName, '')) like :q or lower(coalesce(r.orderName, '')) like :q)
              and (:before is null
                   or r.createdAt < (select b.createdAt from ReturnRequestEntity b where b.id = :before))
            order by r.createdAt desc
            """)
    List<ReturnRequestEntity> search(@Param("status") ReturnStatus status,
                                     @Param("q") String likePattern,
                                     @Param("before") UUID beforeId,
                                     Limit limit);

    List<ReturnRequestEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    @Query("select r.status, count(r) from ReturnRequestEntity r group by r.status")
    List<Object[]> countGroupedByStatus();

    @Query("""
            select l.orderLineId, sum(l.quantity) from ReturnLineEntity l
            where l.request.orderId = :orderId and l.request.status not in :excluded
            group by l.orderLineId
            """)
    List<Object[]> quantitiesByLine(@Param("orderId") UUID orderId,
                                    @Param("excluded") List<ReturnStatus> excluded);

    @Query(value = "select nextval('return_number_seq')", nativeQuery = true)
    long nextNumberValue();
}

interface SpringDataReturnHistoryRepository extends JpaRepository<ReturnHistoryEntity, UUID> {

    List<ReturnHistoryEntity> findByReturnRequestIdOrderByOccurredAt(UUID returnRequestId);
}
