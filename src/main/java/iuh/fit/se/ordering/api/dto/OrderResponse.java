package iuh.fit.se.ordering.api.dto;

import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.shared.domain.TaxMode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OrderResponse(
        Long id,
        Long tableId,
        OrderStatus status,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        TaxMode taxMode,
        Integer taxRateBps,
        Instant taxSnapshotAt,
        Long confirmedById,
        Long servedById,
        String note,
        Instant createdAt,
        Instant confirmedAt,
        Instant readyAt,
        Instant servedAt,
        Instant paidAt,
        Instant cancelledAt,
        Integer latestRevisionNumber,
        List<OrderItemResponse> items
) {

    public static OrderResponse from(Order order, Integer latestRevisionNumber, List<OrderItem> items) {
        return from(order, latestRevisionNumber, items, Map.of());
    }

    public static OrderResponse from(
            Order order,
            Integer latestRevisionNumber,
            List<OrderItem> items,
            Map<Long, String> menuItemNames
    ) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> OrderItemResponse.from(item, menuItemNames))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getTableId(),
                order.getStatus(),
                order.getSubtotalAmount() == null ? BigDecimal.ZERO : order.getSubtotalAmount().toBigDecimal(),
                order.getTaxAmount() == null ? BigDecimal.ZERO : order.getTaxAmount().toBigDecimal(),
                order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount().toBigDecimal(),
                order.getTaxMode(),
                order.getTaxRateBps(),
                order.getTaxSnapshotAt(),
                order.getConfirmedById(),
                order.getServedById(),
                order.getNote(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getReadyAt(),
                order.getServedAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                latestRevisionNumber,
                itemResponses
        );
    }
}
