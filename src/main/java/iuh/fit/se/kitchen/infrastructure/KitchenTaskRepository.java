package iuh.fit.se.kitchen.infrastructure;

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
        where (:status is null or kt.status = :status)
                            and (:fromTime is null or kt.createdAt >= :fromTime)
                            and (:toTime is null or kt.createdAt < :toTime)
        """)
    Page<KitchenTask> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("status") KitchenTaskStatus status,
        Pageable pageable
    );

    List<KitchenTask> findAllByOrderByIdDesc();

    List<KitchenTask> findAllByStatusOrderByIdDesc(KitchenTaskStatus status);

    List<KitchenTask> findAllByStatusInOrderByIdDesc(Collection<KitchenTaskStatus> statuses);

    List<KitchenTask> findAllByOrderItemIdIn(Collection<Long> orderItemIds);

    List<KitchenTask> findTop10ByMenuItemIdAndStatusOrderByCompletedAtDesc(Long menuItemId, KitchenTaskStatus status);

    Optional<KitchenTask> findByOrderItemId(Long orderItemId);

    boolean existsByOrderItemId(Long orderItemId);
}
