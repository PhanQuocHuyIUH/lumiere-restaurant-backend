package iuh.fit.se.shared.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SyncMenuRequest(
        Integer menuItemId,
        String name,
        String description,
        BigDecimal price,
        String category,
        List<String> tags
) {}
