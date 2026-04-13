package iuh.fit.se.serving.application.impl;

import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.serving.application.ServingService;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.security.JwtPrincipal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServingServiceImpl implements ServingService {

    private final OrderingService orderingService;
    private final SimpMessagingTemplate messagingTemplate;

    public ServingServiceImpl(OrderingService orderingService, SimpMessagingTemplate messagingTemplate) {
        this.orderingService = orderingService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void handleKitchenTaskDone(Long orderId) {
        Optional<OrderResponse> readyOrder = orderingService.markOrderReadyIfAllItemsDone(orderId);
        readyOrder.ifPresent(order -> messagingTemplate.convertAndSend(
                "/topic/waiter/ready",
                toWaiterReadyPayload(order)
        ));
    }

    @Override
    public OrderResponse serveItem(Long orderId, Long orderItemId, String idempotencyKey) {
        Long staffId = resolveStaffIdForServing();
        OrderResponse response = orderingService.serveOrderItem(orderId, orderItemId, staffId, idempotencyKey);
        pushTableServingUpdateIfServed(response);
        return response;
    }

    @Override
    public OrderResponse serveAllItems(Long orderId, String idempotencyKey) {
        Long staffId = resolveStaffIdForServing();
        OrderResponse response = orderingService.serveAllOrderItems(orderId, staffId, idempotencyKey);
        pushTableServingUpdateIfServed(response);
        return response;
    }

    private void pushTableServingUpdateIfServed(OrderResponse response) {
        if (response.status() != OrderStatus.SERVED) {
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/tables/" + response.tableId(),
                toTableUpdatePayload(response)
        );
    }

    private Map<String, Object> toWaiterReadyPayload(OrderResponse order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.id());
        payload.put("tableId", order.tableId());
        payload.put("status", order.status().name());
        payload.put("readyAt", order.readyAt());
        return payload;
    }

    private Map<String, Object> toTableUpdatePayload(OrderResponse order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", order.id());
        payload.put("tableId", order.tableId());
        payload.put("status", order.status().name());
        payload.put("servedAt", order.servedAt());
        return payload;
    }

    private Long resolveStaffIdForServing() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new InsufficientAuthenticationException("JWT Authorization is required to serve order items");
        }

        if (!(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new InsufficientAuthenticationException("Invalid authenticated principal");
        }

        if (principal.getStaffId() == null || principal.getStaffId() <= 0) {
            throw new DomainException("Missing staffId in JWT claims");
        }

        return principal.getStaffId();
    }
}
