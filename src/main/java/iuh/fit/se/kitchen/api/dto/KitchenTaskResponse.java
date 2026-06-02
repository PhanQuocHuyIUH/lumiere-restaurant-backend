package iuh.fit.se.kitchen.api.dto;

import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import java.time.Duration;
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
        Instant orderedAt,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Integer actualCookSeconds,
        Long waitedSeconds,
        Integer slaSeconds,
        Boolean slaBreached
) {

    private static final int DEFAULT_COOK_SECONDS = 600;
    private static final int SLA_BUFFER_SECONDS = 300;

    public static KitchenTaskResponse from(KitchenTask task) {
        Instant orderedAt = task.getOrderedAt() != null ? task.getOrderedAt() : task.getCreatedAt();
        int cookSec = task.getExpectedCookTime() != null && task.getExpectedCookTime() > 0
                ? task.getExpectedCookTime()
                : DEFAULT_COOK_SECONDS;
        int slaSec = cookSec + SLA_BUFFER_SECONDS;

        Instant endpoint = switch (task.getStatus()) {
            case DONE, CANCELLED -> task.getCompletedAt() != null ? task.getCompletedAt() : Instant.now();
            default -> Instant.now();
        };
        long waitedSec = orderedAt != null
                ? Math.max(0, Duration.between(orderedAt, endpoint).getSeconds())
                : 0L;
        boolean breached = waitedSec > slaSec
                && task.getStatus() != KitchenTaskStatus.DONE
                && task.getStatus() != KitchenTaskStatus.CANCELLED;

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
                orderedAt,
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getActualCookSeconds(),
                waitedSec,
                slaSec,
                breached
        );
    }
}
