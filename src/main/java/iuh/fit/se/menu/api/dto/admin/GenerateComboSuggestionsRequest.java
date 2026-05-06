package iuh.fit.se.menu.api.dto.admin;

import iuh.fit.se.shared.ai.client.dto.ComboGenerateRequest;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateComboSuggestionsRequest(
        @NotNull(message = "analyzeDays is required")
        @Min(value = 1, message = "analyzeDays must be at least 1")
        @Max(value = 365, message = "analyzeDays must be at most 365")
        Integer analyzeDays,

        @NotNull(message = "minSupport is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "minSupport must be greater than 0")
        @DecimalMax(value = "1.0", message = "minSupport must be less than or equal to 1")
        Double minSupport,

        @NotNull(message = "minConfidence is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "minConfidence must be greater than 0")
        @DecimalMax(value = "1.0", message = "minConfidence must be less than or equal to 1")
        Double minConfidence
) {
    public ComboGenerateRequest toAiRequest() {
        return new ComboGenerateRequest(analyzeDays, minSupport, minConfidence);
    }
}
