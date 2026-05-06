package iuh.fit.se.shared.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AiJobStatusPayload(
        boolean success,
        String jobId,
        String status,
        String message
) {}
