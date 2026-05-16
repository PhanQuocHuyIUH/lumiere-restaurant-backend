package iuh.fit.se.ai.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecommendationLogRequest(
        String sessionId,
        @NotNull List<Integer> shown,
        @NotNull List<Integer> clicked
) {}
