package iuh.fit.se.support.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSupportRequest(
        @NotBlank(message = "message is required")
        String message,

        @NotNull(message = "tableCode is required")
        String tableCode,

        String qrSessionId
) {
}
