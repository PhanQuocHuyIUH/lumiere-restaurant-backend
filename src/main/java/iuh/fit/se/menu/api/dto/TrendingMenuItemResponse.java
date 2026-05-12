package iuh.fit.se.menu.api.dto;

import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuCategory;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import java.math.BigDecimal;

public record TrendingMenuItemResponse(
        Long menuItemId,
        String name,
        String description,
        BigDecimal price,
        boolean available,
        boolean ingredientSufficient,
        MenuItemType itemType,
        ComboKind comboKind,
        Integer cookTime,
        String imageUrl,
        String categoryId,
        String categoryName
) {
    public static TrendingMenuItemResponse from(MenuItem item, MenuCategory category, boolean ingredientSufficient) {
        return new TrendingMenuItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice() == null ? BigDecimal.ZERO : item.getPrice().toBigDecimal(),
                item.isAvailable(),
                ingredientSufficient,
                item.getItemType(),
                item.getComboKind(),
                item.getCookTime(),
                item.getImageUrl(),
                String.valueOf(category != null ? category.getId() : 0),
                category != null ? category.getName() : ""
        );
    }
}
