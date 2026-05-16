package iuh.fit.se.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KitchenTaskInput(
        Long taskId,
        Long menuItemId,
        int quantity,
        String createdAt,
        int cookTimeSeconds
) {}
