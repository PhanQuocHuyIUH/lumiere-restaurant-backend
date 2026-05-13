package iuh.fit.se.ordering.repository;

import iuh.fit.se.ordering.domain.OrderRevision;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRevisionRepository extends JpaRepository<OrderRevision, Long> {

    @Query(value = """
            SELECT DISTINCT ON (orv.order_id)
                orv.order_id AS orderId,
                orv.revision_number AS revisionNumber,
                orv.revision_source AS revisionSource,
                orv.qr_session_id AS qrSessionId,
                orv.created_by_staff AS createdByStaffId,
                orv.created_at AS createdAt
            FROM ordering.order_revisions orv
            WHERE orv.order_id IN (:orderIds)
            ORDER BY orv.order_id, orv.revision_number DESC
            """, nativeQuery = true)
    List<LatestOrderRevisionProjection> findLatestContextByOrderIds(@Param("orderIds") Collection<Long> orderIds);

    Optional<OrderRevision> findTopByOrderIdOrderByRevisionNumberDesc(Long orderId);

    interface LatestOrderRevisionProjection {
        Long getOrderId();

        Integer getRevisionNumber();

        String getRevisionSource();

        String getQrSessionId();

        Long getCreatedByStaffId();

        Instant getCreatedAt();
    }
}
