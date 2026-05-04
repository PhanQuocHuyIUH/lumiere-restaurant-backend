package iuh.fit.se.kitchen.api.dto;

import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import java.time.Instant;

public record KitchenTaskResponse(
        Long id,
    Long orderId,
        Long orderItemId,
    Long tableId,
    Long menuItemId,
    String menuItemName,
    String menuItemImageUrl,
    Integer quantity,
    String orderItemNote,
    String orderNote,
    Integer expectedCookTime,
        KitchenTaskStatus status,
        Instant startedAt,
        Instant completedAt,
        Integer actualCookSeconds
) {

    public static KitchenTaskResponse from(KitchenTask task) {
        return new KitchenTaskResponse(
                task.getId(),
            task.getOrderId(),
                task.getOrderItemId(),
            task.getTableId(),
            task.getMenuItemId(),
            task.getMenuItemName(),
            task.getMenuItemImageUrl(),
            task.getQuantity(),
            task.getOrderItemNote(),
            task.getOrderNote(),
            task.getExpectedCookTime(),
                task.getStatus(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getActualCookSeconds()
        );
    }
}
