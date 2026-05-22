package iuh.fit.se.billing.application.impl;

import iuh.fit.se.billing.api.dto.PaymentRequestResponse;
import iuh.fit.se.billing.application.PaymentRequestService;
import iuh.fit.se.billing.domain.PaymentRequest;
import iuh.fit.se.billing.domain.PaymentRequestMethod;
import iuh.fit.se.billing.domain.PaymentRequestStatus;
import iuh.fit.se.billing.repository.PaymentRequestRepository;
import iuh.fit.se.identity.application.StaffService;
import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.ordering.repository.OrderRepository;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.table.application.TableData;
import iuh.fit.se.table.application.TableService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentRequestServiceImpl implements PaymentRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentRequestServiceImpl.class);

    /**
     * UC008 — eligible order statuses for a customer payment request.
     * Per UC: "Bàn đã có đơn hàng được xác nhận và đang trong trạng thái Đang phục vụ".
     */
    private static final Set<OrderStatus> ELIGIBLE_ORDER_STATUSES = Set.of(OrderStatus.SERVED);

    /** Statuses considered "currently active" — used to find the order at a table for a request. */
    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.CREATED,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.SERVED
    );

    private final PaymentRequestRepository repository;
    private final OrderRepository orderRepository;
    private final TableService tableService;
    private final StaffService staffService;
    private final SimpMessagingTemplate messagingTemplate;

    public PaymentRequestServiceImpl(
            PaymentRequestRepository repository,
            OrderRepository orderRepository,
            TableService tableService,
            StaffService staffService,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.tableService = tableService;
        this.staffService = staffService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public PaymentRequestResponse requestForTable(String tableCode, String qrSessionId, PaymentRequestMethod method) {
        if (method == null) {
            throw new DomainException("preferredMethod is required (CASH or TRANSFER)");
        }
        TableData table = tableService.getTableByCode(tableCode);
        Order order = orderRepository.findTopByTableIdAndStatusInOrderByCreatedAtDesc(table.id(), ACTIVE_ORDER_STATUSES)
                .orElseThrow(() -> new DomainException("Bạn chưa gọi món, không thể yêu cầu thanh toán"));

        if (!ELIGIBLE_ORDER_STATUSES.contains(order.getStatus())) {
            throw new DomainException(
                    "Đơn hàng chưa được phục vụ xong (trạng thái: " + order.getStatus() + ")");
        }

        // Idempotent: if an active request already exists, return it instead of erroring.
        Optional<PaymentRequest> existing = repository.findActiveByOrderId(order.getId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        PaymentRequest pr = PaymentRequest.create(order.getId(), tableCode, qrSessionId, method);
        pr = repository.save(pr);
        publishCreated(pr);
        return toResponse(pr);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentRequestResponse> findActiveByOrderId(Long orderId) {
        return repository.findActiveByOrderId(orderId).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentRequestResponse> findActiveByTableCode(String tableCode) {
        TableData table = tableService.getTableByCode(tableCode);
        return orderRepository
                .findTopByTableIdAndStatusInOrderByCreatedAtDesc(table.id(), ACTIVE_ORDER_STATUSES)
                .flatMap(o -> repository.findActiveByOrderId(o.getId()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRequestResponse> listByStatuses(List<String> statuses) {
        List<PaymentRequestStatus> enums = (statuses == null || statuses.isEmpty())
                ? List.of(PaymentRequestStatus.REQUESTED, PaymentRequestStatus.ACKNOWLEDGED)
                : statuses.stream().map(this::parseStatus).toList();
        return repository.findAllByStatusInOrderByCreatedAtAsc(enums).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PaymentRequestResponse acknowledge(Long requestId, Long staffId) {
        PaymentRequest pr = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentRequest", requestId));
        pr.acknowledge(staffId);
        pr = repository.save(pr);
        publishStatusChange(pr);
        return toResponse(pr);
    }

    @Override
    public PaymentRequestResponse cancel(Long requestId, String reason) {
        PaymentRequest pr = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentRequest", requestId));
        pr.cancel(reason == null || reason.isBlank() ? "CANCELLED_BY_CASHIER" : reason);
        pr = repository.save(pr);
        publishStatusChange(pr);
        return toResponse(pr);
    }

    @Override
    public void completeActiveForOrderQuietly(Long orderId) {
        repository.findActiveByOrderId(orderId).ifPresent(pr -> {
            try {
                pr.complete();
                PaymentRequest saved = repository.save(pr);
                publishStatusChange(saved);
            } catch (Exception ex) {
                LOGGER.warn("Failed to auto-complete PaymentRequest {} for order {}: {}",
                        pr.getId(), orderId, ex.getMessage());
            }
        });
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private PaymentRequestStatus parseStatus(String value) {
        try {
            return PaymentRequestStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new DomainException("Unknown PaymentRequest status: " + value);
        }
    }

    private PaymentRequestResponse toResponse(PaymentRequest pr) {
        String name = null;
        if (pr.getAcknowledgedBy() != null) {
            try {
                name = staffService.getStaffById(pr.getAcknowledgedBy()).getName();
            } catch (Exception ignored) {
                // staff may have been deleted — leave name null
            }
        }
        return PaymentRequestResponse.from(pr, name);
    }

    private void publishCreated(PaymentRequest pr) {
        PaymentRequestResponse payload = toResponse(pr);
        broadcast(payload, "CREATED");
    }

    private void publishStatusChange(PaymentRequest pr) {
        PaymentRequestResponse payload = toResponse(pr);
        broadcast(payload, pr.getStatus().name());
    }

    private void broadcast(PaymentRequestResponse payload, String event) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event", event);
        envelope.put("request", payload);
        try {
            messagingTemplate.convertAndSend("/topic/cashier/payment-requests", envelope);
            messagingTemplate.convertAndSend("/topic/waiter/payment-requests", envelope);
            messagingTemplate.convertAndSend(
                    "/topic/customer/payment-requests/" + payload.tableCode(),
                    envelope
            );
        } catch (Exception ex) {
            LOGGER.warn("Failed to broadcast payment-request event {}: {}", event, ex.getMessage());
        }
    }
}
