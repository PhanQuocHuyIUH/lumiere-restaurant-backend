package iuh.fit.se.shared.ai.api.dto;

import java.time.Instant;

public record AiRuntimeStatusResponse(
        boolean configuredEnabled,
        boolean runtimeEnabled,
        boolean healthy,
        boolean available,
        Instant lastHealthCheckedAt,
        String healthMessage,
        String baseUrl
) {
}
