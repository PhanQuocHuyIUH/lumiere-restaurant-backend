package iuh.fit.se.analytics.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RevenueDetailResponse(
        LocalDate fromDate,
        LocalDate toDate,
        String groupBy,
        BigDecimal totalRevenue,
        BigDecimal totalNetRevenue,
        BigDecimal totalTax,
        Long totalOrders,
        List<RevenuePeriodEntry> periods,
        List<TopMenuItemEntry> topItems,
        Instant generatedAt
) {}
