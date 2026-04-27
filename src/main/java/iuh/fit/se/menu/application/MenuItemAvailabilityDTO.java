package iuh.fit.se.menu.application;

import java.util.List;

public record MenuItemAvailabilityDTO(
        Long menuItemId,
        boolean sufficient,
        List<String> shortageIngredients
) {
    public static MenuItemAvailabilityDTO available(Long menuItemId) {
        return new MenuItemAvailabilityDTO(menuItemId, true, List.of());
    }

    public static MenuItemAvailabilityDTO insufficient(Long menuItemId, List<String> shortages) {
        return new MenuItemAvailabilityDTO(menuItemId, false, shortages);
    }
}
