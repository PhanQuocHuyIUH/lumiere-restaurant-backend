package iuh.fit.se.billing.api.dto;

import iuh.fit.se.billing.domain.PaymentRequest;
import iuh.fit.se.billing.domain.PaymentRequestMethod;
import iuh.fit.se.billing.domain.PaymentRequestStatus;
import java.time.Instant;

public record PaymentRequestResponse(
        Long id,
        Long orderId,
        String tableCode,
        PaymentRequestMethod preferredMethod,
        PaymentRequestStatus status,
        Long acknowledgedBy,
        String acknowledgedByName,
        String cancelledReason,
        Instant createdAt,
        Instant acknowledgedAt,
        Instant completedAt
) {

    public static PaymentRequestResponse from(PaymentRequest pr, String acknowledgedByName) {
        return new PaymentRequestResponse(
                pr.getId(),
                pr.getOrderId(),
                pr.getTableCode(),
                pr.getPreferredMethod(),
                pr.getStatus(),
                pr.getAcknowledgedBy(),
                acknowledgedByName,
                pr.getCancelledReason(),
                pr.getCreatedAt(),
                pr.getAcknowledgedAt(),
                pr.getCompletedAt()
        );
    }
}
