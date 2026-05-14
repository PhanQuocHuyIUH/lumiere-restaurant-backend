package iuh.fit.se.menu.api.dto.manager;

import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.shared.domain.TaxMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateMenuItemRequest(
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

        @NotNull(message = "itemTaxMode is required")
        TaxMode itemTaxMode,

        @NotNull(message = "itemTaxRateBps is required")
        @Min(value = 0, message = "itemTaxRateBps must be >= 0")
        @Max(value = 10000, message = "itemTaxRateBps must be <= 10000")
        Integer itemTaxRateBps
) {
}


