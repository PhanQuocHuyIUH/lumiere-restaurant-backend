package iuh.fit.se.menu.api.dto.manager;

import iuh.fit.se.inventory.domain.IngredientUnit;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecipeItemResponse {

    private Long ingredientId;
    private String ingredientName;
    private IngredientUnit unit;
    private BigDecimal quantity;
}

