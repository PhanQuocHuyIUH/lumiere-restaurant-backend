package iuh.fit.se.shared.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ComboGenerateRequest(
        int analyzeDays,
        double minSupport,
        double minConfidence
) {}
