package iuh.fit.se.menu.application;

import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.menu.domain.ComboKind;
import java.math.BigDecimal;

public record MenuItemDTO(
        Long id,
        String name,
        BigDecimal price,
    String imageUrl,
        boolean available,
        MenuItemType itemType,
        ComboKind comboKind
) {

    public static MenuItemDTO from(MenuItem menuItem) {
        return new MenuItemDTO(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getPrice(),
            menuItem.getImageUrl(),
                menuItem.isAvailable(),
                menuItem.getItemType(),
                menuItem.getComboKind()
        );
    }
}