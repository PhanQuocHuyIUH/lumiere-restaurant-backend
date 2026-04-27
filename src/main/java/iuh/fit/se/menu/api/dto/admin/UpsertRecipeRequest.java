package iuh.fit.se.menu.api.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpsertRecipeRequest(
        @NotEmpty(message = "items must not be empty")
        @Valid
        List<RecipeItemRequest> items
) {
}
