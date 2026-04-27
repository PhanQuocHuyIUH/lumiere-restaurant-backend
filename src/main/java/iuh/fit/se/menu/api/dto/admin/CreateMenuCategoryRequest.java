package iuh.fit.se.menu.api.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record CreateMenuCategoryRequest(
        @NotBlank String name,
        String description,
        Integer displayOrder
) {
}
