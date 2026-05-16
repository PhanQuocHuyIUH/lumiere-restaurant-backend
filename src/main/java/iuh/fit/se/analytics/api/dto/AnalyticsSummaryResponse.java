package iuh.fit.se.analytics.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AnalyticsSummaryResponse(
        Long totalOrders,
        Long confirmedOrders,
        Long cancelledOrders,
        BigDecimal totalRevenue,
        BigDecimal totalNetRevenue,
        BigDecimal totalTax,
        Long successfulPayments,
        Long failedPayments,
        LocalDate fromDate,
        LocalDate toDate,
        Instant generatedAt
) {
}
