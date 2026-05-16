package iuh.fit.se.analytics.api.dto;

import java.math.BigDecimal;

public record RevenuePeriodEntry(
        String periodLabel,
        BigDecimal revenue,
        BigDecimal netRevenue,
        BigDecimal taxAmount,
        Long orderCount
) {}
