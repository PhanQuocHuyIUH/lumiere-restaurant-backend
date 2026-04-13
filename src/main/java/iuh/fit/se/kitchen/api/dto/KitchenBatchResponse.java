package iuh.fit.se.kitchen.api.dto;

import iuh.fit.se.kitchen.domain.BatchSource;
import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record KitchenBatchResponse(
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

    public static KitchenBatchResponse from(KitchenBatch batch) {
        return new KitchenBatchResponse(
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
