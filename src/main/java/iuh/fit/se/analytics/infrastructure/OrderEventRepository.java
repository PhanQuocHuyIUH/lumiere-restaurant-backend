package iuh.fit.se.analytics.infrastructure;

import iuh.fit.se.analytics.domain.OrderEvent;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    @Query("""
        select oe
        from OrderEvent oe
        where (:fromTime is null or oe.createdAt >= :fromTime)
          and (:toTime is null or oe.createdAt < :toTime)
          and (:eventType is null or oe.eventType = :eventType)
        """)
    Page<OrderEvent> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        @Param("eventType") String eventType,
        Pageable pageable
    );

    @Query(value = """
            SELECT
                COUNT(DISTINCT CASE WHEN oe.event_type = 'ORDER_CREATED' THEN oe.order_id END) AS totalOrders,
                COUNT(CASE WHEN oe.event_type = 'ORDER_CONFIRMED' THEN 1 END) AS confirmedOrders,
                COUNT(CASE WHEN oe.event_type = 'ORDER_CANCELLED' THEN 1 END) AS cancelledOrders,
                COALESCE(
                    SUM(
                        CASE
                            WHEN oe.event_type = 'PAYMENT_SUCCESS'
                                THEN COALESCE(NULLIF(oe.metadata ->> 'amount', '')::NUMERIC, 0)
                            ELSE 0
                        END
                    ),
                    0
                ) AS totalRevenue,
                COUNT(CASE WHEN oe.event_type = 'PAYMENT_SUCCESS' THEN 1 END) AS successfulPayments,
                COUNT(CASE WHEN oe.event_type = 'PAYMENT_FAILED' THEN 1 END) AS failedPayments
            FROM analytics.order_events oe
            WHERE (:fromTime IS NULL OR oe.created_at >= :fromTime)
              AND (:toTime IS NULL OR oe.created_at < :toTime)
            """, nativeQuery = true)
    AnalyticsSummaryProjection summarize(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    interface AnalyticsSummaryProjection {
        Long getTotalOrders();

        Long getConfirmedOrders();

        Long getCancelledOrders();

        BigDecimal getTotalRevenue();

        Long getSuccessfulPayments();

        Long getFailedPayments();
    }
}