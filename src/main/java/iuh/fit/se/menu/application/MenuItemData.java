package iuh.fit.se.menu.application;

import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;

public record MenuItemData(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        boolean available,
        boolean ingredientSufficient,
        MenuItemType itemType,
        ComboKind comboKind,
        TaxMode itemTaxMode,
        Integer itemTaxRateBps,
        Integer cookTime
) {

    public static MenuItemData from(MenuItem menuItem) {
        return from(menuItem, true);
    }

    public static MenuItemData from(MenuItem menuItem, boolean ingredientSufficient) {
        return new MenuItemData(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getPrice() == null ? BigDecimal.ZERO : menuItem.getPrice().toBigDecimal(),
                menuItem.getImageUrl(),
                menuItem.isAvailable(),
                ingredientSufficient,
                menuItem.getItemType(),
                menuItem.getComboKind(),
                menuItem.getItemTaxMode(),
                menuItem.getItemTaxRateBps(),
                menuItem.getCookTime()
        );
    }
}
