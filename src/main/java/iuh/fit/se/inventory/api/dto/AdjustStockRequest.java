package iuh.fit.se.inventory.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AdjustStockRequest(
        @NotNull(message = "newQuantity is required")
        @PositiveOrZero(message = "newQuantity must be >= 0")
        BigDecimal newQuantity,

        String note
) {
}
