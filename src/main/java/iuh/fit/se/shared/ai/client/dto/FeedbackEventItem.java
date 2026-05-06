package iuh.fit.se.shared.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record FeedbackEventItem(
        String type,
        List<Integer> shown,
        List<Integer> clicked
) {}
