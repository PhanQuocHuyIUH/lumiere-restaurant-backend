package iuh.fit.se.shared.tax.api.dto;

import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record TaxConfigResponse(
        TaxMode taxMode,
        int taxRateBps,
        BigDecimal taxRatePercent,
        Instant updatedAt,
        Long updatedBy
) {
    public static TaxConfigResponse of(TaxMode taxMode, int taxRateBps, Instant updatedAt, Long updatedBy) {
        BigDecimal percent = BigDecimal.valueOf(taxRateBps)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new TaxConfigResponse(taxMode, taxRateBps, percent, updatedAt, updatedBy);
    }
}
