package iuh.fit.se.shift.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CloseShiftResponse(
        Long shiftId,
        Long cashierId,
        Long openedBy,
        Long closedBy,
        Instant openedAt,
        Instant closedAt,
        BigDecimal openingTotal,
        BigDecimal cashRevenue,
        BigDecimal transferRevenue,
        BigDecimal expectedCash,
        BigDecimal actualCash,
        BigDecimal discrepancy,
        String notes
) {
}
