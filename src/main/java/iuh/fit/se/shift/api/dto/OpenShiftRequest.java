package iuh.fit.se.shift.api.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record OpenShiftRequest(
        @NotNull
        Long cashierId,
        @NotNull
        BigDecimal openingTotal,
        String notes
) {}
