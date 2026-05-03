package iuh.fit.se.menu.api.dto.admin;

import iuh.fit.se.menu.domain.MenuCategory;
import java.time.Instant;

/**
 * Lightweight category entry for admin list view, including item count.
 */
public record AdminMenuCategoryListItemResponse(
        Long id,
        String name,
        String description,
        Integer displayOrder,
        Long itemCount,   // number of items with deleted_at IS NULL (includes unavailable)
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminMenuCategoryListItemResponse from(MenuCategory category, long itemCount) {
        return new AdminMenuCategoryListItemResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                itemCount,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
