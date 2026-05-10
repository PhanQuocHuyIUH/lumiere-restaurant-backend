package iuh.fit.se.billing.listener;

import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.shared.event.PaymentSuccessEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BillingOrderEventListener {

    private final OrderingService orderingService;
    private final SimpMessagingTemplate messagingTemplate;

    public BillingOrderEventListener(OrderingService orderingService, SimpMessagingTemplate messagingTemplate) {
        this.orderingService = orderingService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        OrderResponse order = orderingService.markOrderPaid(event.getOrderId());
        Map<String, Object> payload = toTablePayload(order, event);

        messagingTemplate.convertAndSend(
                "/topic/tables/" + order.tableId(),
                payload
        );

        // Notify waiter POS about payment success (for sound alert + badge update)
        messagingTemplate.convertAndSend(
                "/topic/waiter/payment-success",
                payload
        );
    }

    private Map<String, Object> toTablePayload(OrderResponse order, PaymentSuccessEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.id());
        payload.put("tableId", order.tableId());
        payload.put("status", order.status().name());
        payload.put("paidAt", order.paidAt());
        payload.put("paymentId", event.getPaymentId());
        payload.put("amount", event.getAmount());
        return payload;
    }
}

