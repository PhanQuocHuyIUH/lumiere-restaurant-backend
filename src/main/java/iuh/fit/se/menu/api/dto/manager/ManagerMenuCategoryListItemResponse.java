package iuh.fit.se.menu.api.dto.manager;

import iuh.fit.se.menu.domain.MenuCategory;
import java.time.Instant;

/**
 * Lightweight category entry for manager list view, including item count.
 */
public record ManagerMenuCategoryListItemResponse(
        Long id,
        String name,
        String description,
        Integer displayOrder,
        Long itemCount,   // number of items with deleted_at IS NULL (includes unavailable)
        Instant createdAt,
        Instant updatedAt
) {
    public static ManagerMenuCategoryListItemResponse from(MenuCategory category, long itemCount) {
        return new ManagerMenuCategoryListItemResponse(
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


