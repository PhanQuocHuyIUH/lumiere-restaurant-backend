package iuh.fit.se.inventory.api.dto;

import iuh.fit.se.inventory.domain.Ingredient;
import iuh.fit.se.inventory.domain.IngredientUnit;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IngredientResponse {

    private Long id;
    private String name;
    private IngredientUnit unit;
    private BigDecimal currentQty;
    private BigDecimal lowStockThreshold;
    private boolean lowStock;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;

    public static IngredientResponse from(Ingredient ingredient) {
        return IngredientResponse.builder()
                .id(ingredient.getId())
                .name(ingredient.getName())
                .unit(ingredient.getUnit())
                .currentQty(ingredient.getCurrentQty())
                .lowStockThreshold(ingredient.getLowStockThreshold())
                .lowStock(ingredient.isLowStock())
                .imageUrl(ingredient.getImageUrl())
                .createdAt(ingredient.getCreatedAt())
                .updatedAt(ingredient.getUpdatedAt())
                .build();
    }
}
