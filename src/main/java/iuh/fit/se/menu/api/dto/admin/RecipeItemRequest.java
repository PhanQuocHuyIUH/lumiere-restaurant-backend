package iuh.fit.se.menu.api.dto.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record RecipeItemRequest(
        @NotNull(message = "ingredientId is required")
        Long ingredientId,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be > 0")
        BigDecimal quantity
) {
}
