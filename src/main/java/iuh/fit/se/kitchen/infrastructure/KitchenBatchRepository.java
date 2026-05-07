package iuh.fit.se.kitchen.infrastructure;

import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KitchenBatchRepository extends JpaRepository<KitchenBatch, Long> {

    @Query("""
        select kb
        from KitchenBatch kb
        where (:fromTime is null or kb.createdAt >= :fromTime)
          and (:toTime is null or kb.createdAt < :toTime)
          and (:status is null or kb.status = :status)
        """)
    Page<KitchenBatch> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("status") KitchenBatchStatus status,
        Pageable pageable
    );

    List<KitchenBatch> findAllByOrderByCreatedAtDesc();

    List<KitchenBatch> findAllByStatusOrderByCreatedAtDesc(KitchenBatchStatus status);

    List<KitchenBatch> findAllByStatusAndCreatedAtBefore(KitchenBatchStatus status, Instant time);
}
