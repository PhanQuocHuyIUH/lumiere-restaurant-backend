package iuh.fit.se.ordering.api.dto;

import iuh.fit.se.shared.ai.client.dto.ChatbotRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ChatbotMessageRequest(
        @NotBlank(message = "sessionId is required")
        String sessionId,

        @NotBlank(message = "message is required")
        String message,

        List<@NotNull(message = "currentCartItemIds contains null id") Long> currentCartItemIds
) {
    public ChatbotRequest toAiRequest() {
        return new ChatbotRequest(sessionId.trim(), message.trim(), currentCartItemIds);
    }
}
