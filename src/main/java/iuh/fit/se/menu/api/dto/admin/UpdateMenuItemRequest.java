package iuh.fit.se.menu.api.dto.admin;

import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateMenuItemRequest(
        @NotNull(message = "categoryId is required")
        Long categoryId,

        @NotBlank(message = "name is required")
        String name,

        String description,

        @NotNull(message = "price is required")
        @PositiveOrZero(message = "price must be >= 0")
        BigDecimal price,

        @PositiveOrZero(message = "cookTime must be >= 0")
        Integer cookTime,

        String imageUrl,



        @NotNull(message = "itemType is required")
        MenuItemType itemType,

        ComboKind comboKind,

        Boolean available
) {
}

