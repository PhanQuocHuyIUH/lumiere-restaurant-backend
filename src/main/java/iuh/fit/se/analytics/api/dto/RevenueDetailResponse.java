package iuh.fit.se.analytics.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Detailed revenue analytics response including period breakdown and top-selling items.
 */
public record RevenueDetailResponse(
        LocalDate fromDate,
        LocalDate toDate,
        String groupBy,
        BigDecimal totalRevenue,           // total revenue for the whole period
        Long totalOrders,                   // total paid orders in the period
        List<RevenuePeriodEntry> periods,   // revenue broken down by day/week/month/year
        List<TopMenuItemEntry> topItems,    // top 10 best-selling menu items
        Instant generatedAt
) {}
