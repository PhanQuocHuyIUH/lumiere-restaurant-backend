package iuh.fit.se.support.api.dto;

import iuh.fit.se.support.domain.SupportRequestStatus;
import java.time.Instant;

public record SupportResponse(
        Long id,
        String message,
        String tableCode,
        String qrSessionId,
        SupportRequestStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
