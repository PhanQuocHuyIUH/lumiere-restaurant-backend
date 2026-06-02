package iuh.fit.se.kitchen.repository;

import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KitchenTaskRepository extends JpaRepository<KitchenTask, Long> {

    @Query("""
        select kt
        from KitchenTask kt
        where kt.createdAt >= :fromTime
                            and kt.createdAt < :toTime
                            and (:filterByStatus = false or kt.status = :status)
        """)
    Page<KitchenTask> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("filterByStatus") boolean filterByStatus,
        @Param("status") KitchenTaskStatus status,
        Pageable pageable
    );

    List<KitchenTask> findAllByOrderByIdDesc();

    List<KitchenTask> findAllByStatusOrderByIdDesc(KitchenTaskStatus status);

    List<KitchenTask> findAllByStatusInOrderByIdDesc(Collection<KitchenTaskStatus> statuses);

    Page<KitchenTask> findAllByStatusOrderByIdDesc(KitchenTaskStatus status, Pageable pageable);

    Page<KitchenTask> findAllByStatusInOrderByIdDesc(Collection<KitchenTaskStatus> statuses, Pageable pageable);

    List<KitchenTask> findAllByOrderItemIdIn(Collection<Long> orderItemIds);

    List<KitchenTask> findTop10ByMenuItemIdAndStatusOrderByCompletedAtDesc(Long menuItemId, KitchenTaskStatus status);

    Optional<KitchenTask> findByOrderItemId(Long orderItemId);

    boolean existsByOrderItemId(Long orderItemId);

    List<KitchenTask> findAllByOrderIdAndStatusIn(Long orderId, Collection<KitchenTaskStatus> statuses);

    /**
     * Aggregate kitchen SLA stats for DONE tasks in [from, to).
     * Wait time = completed_at - ordered_at. Breach = wait > expected_cook + 300s buffer.
     */
    @Query(value = """
        select
            count(*) as totalTasks,
            count(*) filter (where extract(epoch from (completed_at - ordered_at)) > (coalesce(expected_cook_time, 600) + 300)) as breachedTasks,
            coalesce(avg(extract(epoch from (completed_at - ordered_at))), 0) as avgWaitSeconds,
            coalesce(percentile_cont(0.95) within group (order by extract(epoch from (completed_at - ordered_at))), 0) as p95WaitSeconds,
            coalesce(max(extract(epoch from (completed_at - ordered_at))), 0) as maxWaitSeconds
        from kitchen.kitchen_tasks
        where status = 'DONE'
                            and completed_at is not null
                            and ordered_at is not null
                            and completed_at >= :fromTime
                            and completed_at < :toTime
        """, nativeQuery = true)
    KitchenSlaStatsProjection aggregateSlaStats(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    interface KitchenSlaStatsProjection {
        Long getTotalTasks();
        Long getBreachedTasks();
        Double getAvgWaitSeconds();
        Double getP95WaitSeconds();
        Double getMaxWaitSeconds();
    }
}

