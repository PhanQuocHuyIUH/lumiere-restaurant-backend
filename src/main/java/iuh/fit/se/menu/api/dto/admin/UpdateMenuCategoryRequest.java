package iuh.fit.se.menu.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMenuCategoryRequest(
        @NotBlank String name,
        String description,
        @NotNull Integer displayOrder
) {
}
