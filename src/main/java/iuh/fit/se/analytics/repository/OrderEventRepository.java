package iuh.fit.se.analytics.repository;

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
                where oe.createdAt >= :fromTime
                    and oe.createdAt < :toTime
                    and (:filterByEventType = false or oe.eventType = :eventType)
        """)
    Page<OrderEvent> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
                @Param("filterByEventType") boolean filterByEventType,
        @Param("eventType") String eventType,
        Pageable pageable
    );

    @Query(value = """
            SELECT
                COUNT(DISTINCT o.id)                                                                AS totalOrders,
                COUNT(DISTINCT CASE WHEN o.confirmed_at IS NOT NULL THEN o.id END)                 AS confirmedOrders,
                COUNT(DISTINCT CASE WHEN o.status = 'CANCELLED' THEN o.id END)                     AS cancelledOrders,
                COALESCE(SUM(CASE WHEN p.status = 'SUCCESS' THEN p.amount_vnd END), 0)             AS totalRevenue,
                COUNT(DISTINCT CASE WHEN p.status = 'SUCCESS' THEN p.id END)                       AS successfulPayments,
                COUNT(DISTINCT CASE WHEN p.status = 'FAILED'  THEN p.id END)                       AS failedPayments
            FROM ordering.orders o
            LEFT JOIN payment.payments p ON p.order_id = o.id
            WHERE (CAST(:fromTime AS timestamptz) IS NULL OR o.created_at >= CAST(:fromTime AS timestamptz))
              AND (CAST(:toTime AS timestamptz) IS NULL OR o.created_at < CAST(:toTime AS timestamptz))
            """, nativeQuery = true)
    AnalyticsSummaryProjection summarize(@Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    /**
     * Aggregates revenue and order counts grouped by a PostgreSQL DATE_TRUNC granularity
     * ('day', 'week', 'month', 'year'). Source: payment.payments WHERE status = 'SUCCESS'.
     */
    @Query(value = """
            SELECT
                DATE_TRUNC(:granularity, p.paid_at AT TIME ZONE 'UTC') AS period,
                COALESCE(SUM(p.amount_vnd), 0)                          AS revenue,
                COUNT(p.id)                                             AS order_count
            FROM payment.payments p
            WHERE p.status = 'SUCCESS'
              AND (CAST(:fromTime AS timestamptz) IS NULL OR p.paid_at >= CAST(:fromTime AS timestamptz))
              AND (CAST(:toTime AS timestamptz) IS NULL OR p.paid_at < CAST(:toTime AS timestamptz))
            GROUP BY period
            ORDER BY period
            """, nativeQuery = true)
    java.util.List<RevenuePeriodProjection> findRevenueByPeriod(
            @Param("granularity") String granularity,
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

    /**
     * Top 10 best-selling menu items by total quantity within PAID orders.
     * Cross-schema JOIN: ordering.orders -> ordering.order_revisions
     *   -> ordering.order_items -> menu.menu_items.
     */
    @Query(value = """
            SELECT
                oi.menu_item_id                 AS menu_item_id,
                mi.name                         AS menu_item_name,
                COALESCE(SUM(oi.quantity), 0)   AS total_quantity,
                COUNT(DISTINCT o.id)            AS order_count,
                COALESCE(SUM(oi.subtotal_vnd), 0) AS total_revenue
            FROM ordering.orders o
            JOIN ordering.order_revisions orv ON orv.order_id = o.id
            JOIN ordering.order_items     oi  ON oi.revision_id = orv.id
            JOIN menu.menu_items           mi  ON mi.id = oi.menu_item_id
            WHERE o.status = 'PAID'
              AND oi.is_billable = true
              AND oi.menu_item_id IS NOT NULL
              AND (CAST(:fromTime AS timestamptz) IS NULL OR o.paid_at >= CAST(:fromTime AS timestamptz))
              AND (CAST(:toTime AS timestamptz) IS NULL OR o.paid_at < CAST(:toTime AS timestamptz))
            GROUP BY oi.menu_item_id, mi.name
            ORDER BY total_quantity DESC
            LIMIT 10
            """, nativeQuery = true)
    java.util.List<TopMenuItemProjection> findTop10MenuItems(
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

    /**
     * Order count per menu item within PAID orders — no LIMIT, used for trending scoring.
     */
    @Query(value = """
            SELECT
                oi.menu_item_id                 AS menu_item_id,
                COUNT(DISTINCT o.id)            AS order_count
            FROM ordering.orders o
            JOIN ordering.order_revisions orv ON orv.order_id = o.id
            JOIN ordering.order_items     oi  ON oi.revision_id = orv.id
            WHERE o.status = 'PAID'
              AND oi.is_billable = true
              AND oi.menu_item_id IS NOT NULL
              AND (CAST(:fromTime AS timestamptz) IS NULL OR o.paid_at >= CAST(:fromTime AS timestamptz))
              AND (CAST(:toTime AS timestamptz) IS NULL OR o.paid_at < CAST(:toTime AS timestamptz))
            GROUP BY oi.menu_item_id
            """, nativeQuery = true)
    java.util.List<ItemOrderCountProjection> findOrderCountsByItem(
            @Param("fromTime") Instant fromTime,
            @Param("toTime") Instant toTime
    );

    interface AnalyticsSummaryProjection {
        Long getTotalOrders();

        Long getConfirmedOrders();

        Long getCancelledOrders();

        BigDecimal getTotalRevenue();

        Long getSuccessfulPayments();

        Long getFailedPayments();
    }

    interface RevenuePeriodProjection {
        Instant getPeriod();

        BigDecimal getRevenue();

        Long getOrderCount();
    }

    interface TopMenuItemProjection {
        Long getMenuItemId();

        String getMenuItemName();

        Long getTotalQuantity();

        Long getOrderCount();

        BigDecimal getTotalRevenue();
    }

    interface ItemOrderCountProjection {
        Long getMenuItemId();

        Long getOrderCount();
    }
}