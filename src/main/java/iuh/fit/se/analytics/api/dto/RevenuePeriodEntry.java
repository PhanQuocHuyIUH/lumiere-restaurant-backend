package iuh.fit.se.analytics.api.dto;

import java.math.BigDecimal;

/**
 * Revenue and order count aggregated for a single time period.
 */
public record RevenuePeriodEntry(
        String periodLabel,   // e.g. "2025-04-30", "2025-W17", "2025-04", "2025"
        BigDecimal revenue,
        Long orderCount
) {}
