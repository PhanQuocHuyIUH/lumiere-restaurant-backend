package iuh.fit.se.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WasteLotRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 500)
        String reason
) {
}
