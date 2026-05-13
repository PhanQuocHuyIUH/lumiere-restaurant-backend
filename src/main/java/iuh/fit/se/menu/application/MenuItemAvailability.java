package iuh.fit.se.menu.application;

import java.util.List;

public record MenuItemAvailability(
        Long menuItemId,
        boolean sufficient,
        List<String> shortageIngredients
) {
    public static MenuItemAvailability available(Long menuItemId) {
        return new MenuItemAvailability(menuItemId, true, List.of());
    }

    public static MenuItemAvailability insufficient(Long menuItemId, List<String> shortages) {
        return new MenuItemAvailability(menuItemId, false, shortages);
    }
}
