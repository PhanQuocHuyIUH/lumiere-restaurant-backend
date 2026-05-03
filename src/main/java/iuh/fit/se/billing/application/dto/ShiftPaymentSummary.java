package iuh.fit.se.billing.application.dto;

import java.math.BigDecimal;

public record ShiftPaymentSummary(
        Long shiftId,
        BigDecimal cashRevenue,
        BigDecimal transferRevenue,
        long totalBills
) {
}
