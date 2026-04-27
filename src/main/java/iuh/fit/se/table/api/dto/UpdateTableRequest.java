package iuh.fit.se.table.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTableRequest(
        @NotNull(message = "floor is required")
        @Min(value = 1, message = "floor must be >= 1")
        Integer floor,

        @NotNull(message = "tableNo is required")
        @Min(value = 1, message = "tableNo must be >= 1")
        Integer tableNo,

        @NotNull(message = "capacity is required")
        @Min(value = 1, message = "capacity must be >= 1")
        Integer capacity
) {
}
