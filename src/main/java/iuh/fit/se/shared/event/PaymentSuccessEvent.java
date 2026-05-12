package iuh.fit.se.shared.event;

import java.math.BigDecimal;
import lombok.Getter;

@Getter
public final class PaymentSuccessEvent extends DomainEvent {

    private final Long paymentId;
    private final Long orderId;
    private final BigDecimal subtotalAmount;
    private final BigDecimal taxAmount;
    private final BigDecimal amount;

    public PaymentSuccessEvent(Long paymentId, Long orderId, BigDecimal subtotalAmount, BigDecimal taxAmount, BigDecimal amount) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.subtotalAmount = subtotalAmount;
        this.taxAmount = taxAmount;
        this.amount = amount;
    }
}
