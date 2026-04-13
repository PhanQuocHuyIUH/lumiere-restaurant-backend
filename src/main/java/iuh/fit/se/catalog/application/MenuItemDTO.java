package iuh.fit.se.catalog.application;

import iuh.fit.se.catalog.domain.MenuItem;
import java.math.BigDecimal;

public record MenuItemDTO(
        Long id,
        String name,
        BigDecimal price,
        boolean available
) {

    public static MenuItemDTO from(MenuItem menuItem) {
        return new MenuItemDTO(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getPrice(),
                menuItem.isAvailable()
        );
    }
}