package iuh.fit.se.menu.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpsertPickComboRequest(
        @NotEmpty(message = "slots must not be empty")
        @Valid
        List<Slot> slots
) {
    public record Slot(
            @NotBlank(message = "name is required")
            String name,

            @NotNull(message = "minSelect is required")
            @Min(value = 0, message = "minSelect must be >= 0")
            Integer minSelect,

            @NotNull(message = "maxSelect is required")
            @Min(value = 0, message = "maxSelect must be >= 0")
            Integer maxSelect,

            @NotNull(message = "displayOrder is required")
            Integer displayOrder,

            @NotEmpty(message = "allowedItemIds must not be empty")
            List<Long> allowedItemIds
    ) {
    }
}

