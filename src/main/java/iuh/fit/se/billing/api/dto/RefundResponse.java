package iuh.fit.se.billing.api.dto;

import iuh.fit.se.billing.domain.Refund;
import iuh.fit.se.billing.domain.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        Long id,
        Long paymentId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        String providerRefundId,
        Long requestedBy,
        String requestedByName,
        Instant createdAt,
        Instant completedAt
) {

    public static RefundResponse from(Refund refund, String requestedByName) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getAmount() == null ? BigDecimal.ZERO : refund.getAmount().toBigDecimal(),
                refund.getReason(),
                refund.getStatus(),
                refund.getProviderRefundId(),
                refund.getRequestedBy(),
                requestedByName,
                refund.getCreatedAt(),
                refund.getCompletedAt()
        );
    }
}
