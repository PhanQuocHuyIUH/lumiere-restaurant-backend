package iuh.fit.se.shared.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KitchenBatchSuggestion(
        Long menuItemId,
        List<Long> taskIds,
        double estimatedSavingMinutes,
        String reason
) {}
