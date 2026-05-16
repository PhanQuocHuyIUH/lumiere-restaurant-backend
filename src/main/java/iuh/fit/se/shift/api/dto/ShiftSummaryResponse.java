package iuh.fit.se.shift.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ShiftSummaryResponse(
        Long shiftId,
        Long cashierId,
        Instant openedAt,
        BigDecimal openingTotal,
        BigDecimal cashRevenue,
        BigDecimal transferRevenue,
        BigDecimal expectedCash,
        long totalBills
) {
}
