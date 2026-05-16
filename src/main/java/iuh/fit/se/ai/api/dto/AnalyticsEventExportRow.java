package iuh.fit.se.ai.api.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import iuh.fit.se.analytics.domain.OrderEvent;
import java.time.Instant;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AnalyticsEventExportRow(
        Long id,
        Long orderId,
        String eventType,
        String eventSource,
        Long actorId,
        String actorType,
        Map<String, Object> metadata,
        Instant createdAt
) {

    public static AnalyticsEventExportRow from(OrderEvent event) {
        return new AnalyticsEventExportRow(
                event.getId(),
                event.getOrderId(),
                event.getEventType(),
                event.getEventSource(),
                event.getActorId(),
                event.getActorType(),
                event.getMetadata(),
                event.getCreatedAt()
        );
    }
}
