package iuh.fit.se.shared.event;

import lombok.Getter;

@Getter
public final class PaymentFailedEvent extends DomainEvent {

    private final Long paymentId;
    private final Long orderId;
    private final String reason;

    public PaymentFailedEvent(Long paymentId, Long orderId, String reason) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.reason = reason;
    }
}
