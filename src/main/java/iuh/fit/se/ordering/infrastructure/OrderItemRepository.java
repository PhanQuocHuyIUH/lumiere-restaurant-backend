package iuh.fit.se.ordering.infrastructure;

import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderItemStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
        select oi
        from OrderItem oi
        where oi.createdAt >= :fromTime
          and oi.createdAt < :toTime
          and (:filterByStatus = false or oi.status = :status)
        """)
    Page<OrderItem> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("filterByStatus") boolean filterByStatus,
        @Param("status") OrderItemStatus status,
        Pageable pageable
    );

    List<OrderItem> findAllByRevisionIdOrderByIdAsc(Long revisionId);

    @Query("""
            select coalesce(sum(oi.subtotal), 0)
            from OrderItem oi
            where oi.revisionId = :revisionId
              and oi.billable = true
            """)
    BigDecimal sumSubtotalByRevisionId(Long revisionId);

    List<OrderItem> findAllByRevisionIdAndComboParentFalseOrderByIdAsc(Long revisionId);
}
