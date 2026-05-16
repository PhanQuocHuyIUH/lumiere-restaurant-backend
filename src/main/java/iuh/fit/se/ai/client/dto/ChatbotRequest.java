package iuh.fit.se.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ChatbotRequest(
        String sessionId,
        String message,
        List<Long> currentCartItemIds,
        List<ConversationTurn> conversationHistory
) {
    public ChatbotRequest(String sessionId, String message, List<Long> currentCartItemIds) {
        this(sessionId, message, currentCartItemIds, List.of());
    }
}
