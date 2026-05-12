package iuh.fit.se.shared.tax.api.dto;

import iuh.fit.se.shared.domain.TaxMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TaxConfigUpdateRequest(
        @NotNull(message = "taxMode is required")
        TaxMode taxMode,

        @NotNull(message = "taxRateBps is required")
        @Min(value = 0, message = "taxRateBps must be >= 0")
        @Max(value = 10000, message = "taxRateBps must be <= 10000 (100%)")
        Integer taxRateBps
) {}
