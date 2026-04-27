package iuh.fit.se.ordering.infrastructure;

import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        select o
        from Order o
        where (:fromTime is null or o.createdAt >= :fromTime)
          and (:toTime is null or o.createdAt < :toTime)
          and (:status is null or o.status = :status)
        """)
    Page<Order> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("status") OrderStatus status,
        Pageable pageable
    );

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

    Optional<Order> findTopByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId, Collection<OrderStatus> statuses);

    boolean existsByTableIdAndStatusIn(Long tableId, Collection<OrderStatus> statuses);

    boolean existsByTableIdAndStatusInAndIdNot(Long tableId, Collection<OrderStatus> statuses, Long id);
}
