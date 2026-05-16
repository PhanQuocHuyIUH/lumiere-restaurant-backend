package iuh.fit.se.ai.api.dto;

import iuh.fit.se.ai.client.dto.RecommendRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecommendMenuRequest(
        @NotEmpty(message = "currentItems must not be empty")
        List<@NotNull(message = "currentItems contains null id") Long> currentItems,

        @NotNull(message = "topK is required")
        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK must be at most 20")
        Integer topK
) {
    public RecommendRequest toAiRequest() {
        return new RecommendRequest(currentItems, topK);
    }
}
