package iuh.fit.se.kitchen.api.dto;

import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import java.time.Instant;

public record KitchenTaskResponse(
        Long id,
        Long orderItemId,
        KitchenTaskStatus status,
        String staffNote,
        Instant startedAt,
        Instant completedAt,
        Integer actualCookSeconds
) {

    public static KitchenTaskResponse from(KitchenTask task) {
        return new KitchenTaskResponse(
                task.getId(),
                task.getOrderItemId(),
                task.getStatus(),
                task.getStaffNote(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getActualCookSeconds()
        );
    }
}
