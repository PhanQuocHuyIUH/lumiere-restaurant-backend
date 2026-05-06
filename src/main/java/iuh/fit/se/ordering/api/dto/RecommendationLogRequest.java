package iuh.fit.se.ordering.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RecommendationLogRequest(
        @NotNull List<Integer> shown,
        @NotNull List<Integer> clicked
) {}
