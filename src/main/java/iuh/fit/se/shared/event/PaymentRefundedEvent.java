package iuh.fit.se.shared.event;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Published after a refund reaches SUCCESS. Carries enough context for listeners
 * to cancel the order (full refund) or just emit a WS notification (partial).
 */
@Getter
public final class PaymentRefundedEvent extends DomainEvent {

    private final Long paymentId;
    private final Long orderId;
    private final Long refundId;
    private final BigDecimal refundAmount;
    private final BigDecimal totalRefundedAmount;
    private final BigDecimal paymentAmount;
    private final boolean fullRefund;
    private final String reason;
    private final Long requestedBy;

    public PaymentRefundedEvent(
            Long paymentId,
            Long orderId,
            Long refundId,
            BigDecimal refundAmount,
            BigDecimal totalRefundedAmount,
            BigDecimal paymentAmount,
            boolean fullRefund,
            String reason,
            Long requestedBy
    ) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.refundId = refundId;
        this.refundAmount = refundAmount;
        this.totalRefundedAmount = totalRefundedAmount;
        this.paymentAmount = paymentAmount;
        this.fullRefund = fullRefund;
        this.reason = reason;
        this.requestedBy = requestedBy;
    }
}
