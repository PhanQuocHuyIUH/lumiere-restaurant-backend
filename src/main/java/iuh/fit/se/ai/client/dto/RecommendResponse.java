package iuh.fit.se.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RecommendResponse(
        boolean success,
        String source,
        List<RecommendItem> items,
        String modelVersion
) {}
