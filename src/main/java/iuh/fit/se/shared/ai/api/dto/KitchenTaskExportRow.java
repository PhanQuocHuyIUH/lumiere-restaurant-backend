package iuh.fit.se.shared.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KitchenTaskExportRow(
        Long id,
        Long orderItemId,
        KitchenTaskStatus status,
    Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Integer actualCookSeconds
) {

    public static KitchenTaskExportRow from(KitchenTask task) {
        return new KitchenTaskExportRow(
                task.getId(),
                task.getOrderItemId(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getActualCookSeconds()
        );
    }
}
