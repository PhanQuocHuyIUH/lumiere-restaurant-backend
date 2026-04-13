package iuh.fit.se.ordering.api.dto;

import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderItemStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderItemResponse(
        Long id,
        Long revisionId,
        Long menuItemId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String note,
        OrderItemStatus status,
        Instant createdAt
) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getRevisionId(),
                item.getMenuItemId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.calculateSubtotal(),
                item.getNote(),
                item.getStatus(),
                item.getCreatedAt()
        );
    }
}
