package iuh.fit.se.inventory.api.dto;

import iuh.fit.se.inventory.domain.IngredientUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateIngredientRequest(
        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "unit is required")
        IngredientUnit unit,

        @PositiveOrZero(message = "lowStockThreshold must be >= 0")
        BigDecimal lowStockThreshold,

        String imageUrl
) {
}
