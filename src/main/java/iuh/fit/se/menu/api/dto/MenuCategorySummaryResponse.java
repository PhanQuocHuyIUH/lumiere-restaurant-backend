package iuh.fit.se.menu.api.dto;

import iuh.fit.se.menu.domain.MenuCategory;

/**
 * Lightweight category entry for staff menu browsing.
 */
public record MenuCategorySummaryResponse(
        Long id,
        String name,
        String description,
        Integer displayOrder
) {
    public static MenuCategorySummaryResponse from(MenuCategory category) {
        return new MenuCategorySummaryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder()
        );
    }
}