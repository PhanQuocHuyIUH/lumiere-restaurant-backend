package iuh.fit.se.shared.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.domain.MenuItemType;
import java.math.BigDecimal;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MenuItemExportRow(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category,
        MenuItemType itemType,
        Integer cookTime,
        boolean available,
        Instant createdAt,
        Instant updatedAt
) {
    public static MenuItemExportRow from(MenuItem item, String categoryName) {
        return new MenuItemExportRow(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                categoryName,
                item.getItemType(),
                item.getCookTime(),
                item.isAvailable(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
