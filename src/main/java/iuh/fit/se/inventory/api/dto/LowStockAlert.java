package iuh.fit.se.inventory.api.dto;

import iuh.fit.se.inventory.domain.Ingredient;
import iuh.fit.se.inventory.domain.IngredientUnit;
import java.math.BigDecimal;

public record LowStockAlert(
        Long ingredientId,
        String name,
        BigDecimal currentQty,
        BigDecimal threshold,
        IngredientUnit unit
) {
    public static LowStockAlert from(Ingredient ingredient) {
        return new LowStockAlert(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getCurrentQty(),
                ingredient.getLowStockThreshold(),
                ingredient.getUnit()
        );
    }
}
