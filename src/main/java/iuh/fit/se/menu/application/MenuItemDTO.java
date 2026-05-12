package iuh.fit.se.menu.application;

import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;

public record MenuItemDTO(
        Long id,
        String name,
        BigDecimal price,
        String imageUrl,
        boolean available,
        MenuItemType itemType,
        ComboKind comboKind,
        TaxMode itemTaxMode,
        Integer itemTaxRateBps
) {

    public static MenuItemDTO from(MenuItem menuItem) {
        return new MenuItemDTO(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getPrice() == null ? BigDecimal.ZERO : menuItem.getPrice().toBigDecimal(),
                menuItem.getImageUrl(),
                menuItem.isAvailable(),
                menuItem.getItemType(),
                menuItem.getComboKind(),
                menuItem.getItemTaxMode(),
                menuItem.getItemTaxRateBps()
        );
    }
}