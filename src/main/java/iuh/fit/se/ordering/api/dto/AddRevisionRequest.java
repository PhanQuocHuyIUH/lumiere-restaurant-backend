package iuh.fit.se.ordering.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AddRevisionRequest(
        String note,

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<RevisionItemRequest> items
) {

    public record RevisionItemRequest(
            @NotNull(message = "menuItemId is required")
            Long menuItemId,

            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be greater than 0")
            Integer quantity,

                        String note
    ) {
    }
}
