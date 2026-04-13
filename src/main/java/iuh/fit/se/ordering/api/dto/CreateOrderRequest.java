package iuh.fit.se.ordering.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "tableCode is required")
        String tableCode,

        String note,

        Boolean splitBillAllowed,

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<OrderItemRequest> items
) {

    public record OrderItemRequest(
            @NotNull(message = "menuItemId is required")
            Long menuItemId,

            @NotNull(message = "quantity is required")
            @Min(value = 1, message = "quantity must be greater than 0")
            Integer quantity,

                        String note
    ) {
    }
}
