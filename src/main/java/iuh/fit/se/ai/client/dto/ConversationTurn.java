package iuh.fit.se.ai.client.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ConversationTurn(
        String role,
        String content
) {
    public static ConversationTurn user(String content) {
        return new ConversationTurn("user", content);
    }

    public static ConversationTurn assistant(String content) {
        return new ConversationTurn("assistant", content);
    }
}
