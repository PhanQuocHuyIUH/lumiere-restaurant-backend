package iuh.fit.se.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.kitchen.domain.BatchSource;
import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import java.math.BigDecimal;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KitchenBatchExportRow(
        Long id,
        Long menuItemId,
        Integer quantity,
        KitchenBatchStatus status,
        BatchSource source,
        BigDecimal aiConfidence,
        Integer estimatedSavingMinutes,
        String batchNote,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {

    public static KitchenBatchExportRow from(KitchenBatch batch) {
        return new KitchenBatchExportRow(
                batch.getId(),
                batch.getMenuItemId(),
                batch.getQuantity(),
                batch.getStatus(),
                batch.getSource(),
                batch.getAiConfidence(),
                batch.getEstimatedSavingMinutes(),
                batch.getBatchNote(),
                batch.getStartedAt(),
                batch.getCompletedAt(),
                batch.getCreatedAt()
        );
    }
}
