package iuh.fit.se.inventory.application;

import iuh.fit.se.inventory.domain.IngredientUnit;
import java.math.BigDecimal;

public record IngredientData(
        Long id,
        String name,
        IngredientUnit unit,
        BigDecimal currentQty
) {}
