package iuh.fit.se.billing.listener;

import iuh.fit.se.billing.application.PaymentRequestService;
import iuh.fit.se.billing.domain.Payment;
import iuh.fit.se.billing.repository.PaymentRepository;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.shared.event.PaymentRefundedEvent;
import iuh.fit.se.shared.event.PaymentSuccessEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BillingOrderEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingOrderEventListener.class);

    private final OrderingService orderingService;
    private final PaymentRequestService paymentRequestService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PaymentRepository paymentRepository;
    private final TableService tableService;

    public BillingOrderEventListener(
            OrderingService orderingService,
            PaymentRequestService paymentRequestService,
            SimpMessagingTemplate messagingTemplate,
            PaymentRepository paymentRepository,
            TableService tableService
    ) {
        this.orderingService = orderingService;
        this.paymentRequestService = paymentRequestService;
        this.messagingTemplate = messagingTemplate;
        this.paymentRepository = paymentRepository;
        this.tableService = tableService;
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

        // Auto-complete any active customer payment-request once the order is actually paid.
        paymentRequestService.completeActiveForOrderQuietly(event.getOrderId());

        // Group ("gộp bàn") settlement: a group anchor payment settles every member order at once.
        // Only the anchor Payment carries a table_group_id — normal per-table payments skip this.
        Long groupId = paymentRepository.findById(event.getPaymentId())
                .map(Payment::getTableGroupId)
                .orElse(null);
        if (groupId != null) {
            try {
                orderingService.markGroupOrdersPaid(groupId);
                tableService.closeTableGroup(groupId);
            } catch (Exception ex) {
                // Money is already captured; a failure here (e.g. a leftover unserved order) must not
                // roll back the payment. Surface it for manual cleanup instead.
                LOGGER.warn("Failed to finalize group {} after anchor payment {}: {}",
                        groupId, event.getPaymentId(), ex.getMessage());
            }
        }
    }

    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        if (event.isFullRefund()) {
            String reason = "REFUND_FULL: "
                    + (event.getReason() == null || event.getReason().isBlank()
                        ? "no reason provided"
                        : event.getReason());
            try {
                orderingService.cancelOrder(event.getOrderId(), reason);
            } catch (Exception ex) {
                // Order may have been cancelled already, or not in a cancellable state.
                // Log + continue — the refund itself is already persisted.
                LOGGER.warn("Failed to cancel order {} after full refund: {}",
                        event.getOrderId(), ex.getMessage());
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("paymentId", event.getPaymentId());
        payload.put("refundId", event.getRefundId());
        payload.put("refundAmount", event.getRefundAmount());
        payload.put("totalRefundedAmount", event.getTotalRefundedAmount());
        payload.put("paymentAmount", event.getPaymentAmount());
        payload.put("fullRefund", event.isFullRefund());
        payload.put("reason", event.getReason());

        messagingTemplate.convertAndSend("/topic/cashier/refund", payload);
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

