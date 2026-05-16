package iuh.fit.se.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.kitchen.domain.BatchPerformance;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BatchPerformanceExportRow(
        Long id,
        Long batchId,
        Integer baselineMinutes,
        Integer actualMinutes,
        Integer savingMinutes,
        Instant recordedAt
) {

    public static BatchPerformanceExportRow from(BatchPerformance performance) {
        return new BatchPerformanceExportRow(
                performance.getId(),
                performance.getBatchId(),
                performance.getBaselineMinutes(),
                performance.getActualMinutes(),
                performance.getSavingMinutes(),
                performance.getRecordedAt()
        );
    }
}
