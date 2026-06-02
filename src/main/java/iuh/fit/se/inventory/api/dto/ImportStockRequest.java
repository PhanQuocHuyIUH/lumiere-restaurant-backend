package iuh.fit.se.inventory.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportStockRequest(
        @NotNull(message = "ingredientId is required")
        Long ingredientId,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be > 0")
        BigDecimal quantity,

        @NotNull(message = "expiryDate is required")
        @Future(message = "expiryDate must be in the future")
        LocalDate expiryDate,

        String note
) {
}
