package iuh.fit.se.menu.api.dto.manager;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpsertFixedComboRequest(
        @NotEmpty(message = "components must not be empty")
        @Valid
        List<Component> components
) {
    public record Component(
            @NotNull(message = "menuItemId is required")
            Long menuItemId,

            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be >= 1")
            Integer quantity
    ) {
    }
}


