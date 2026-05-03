package iuh.fit.se.shift.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CloseShiftRequest(
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal actualCash,
        String notes
) {
}
