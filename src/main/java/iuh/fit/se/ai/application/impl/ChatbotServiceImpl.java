package iuh.fit.se.ai.application.impl;

import iuh.fit.se.ai.application.ChatbotService;
import iuh.fit.se.ai.AiClient;
import iuh.fit.se.ai.AiOperation;
import iuh.fit.se.ai.client.dto.ChatbotRequest;
import iuh.fit.se.ai.client.dto.ChatbotResponse;
import iuh.fit.se.ai.client.dto.ConversationTurn;
import iuh.fit.se.shared.exception.DomainException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private final AiClient aiClient;
    private final ChatbotHistoryCacheService chatbotHistoryCache;

    public ChatbotServiceImpl(AiClient aiClient, ChatbotHistoryCacheService chatbotHistoryCache) {
        this.aiClient = aiClient;
        this.chatbotHistoryCache = chatbotHistoryCache;
    }

    @Override
    public ChatbotResponse chat(ChatbotRequest request) {
        ChatbotRequest safeRequest = normalize(request);

        List<ConversationTurn> history = chatbotHistoryCache.load(safeRequest.sessionId());
        ChatbotRequest requestWithHistory = new ChatbotRequest(
                safeRequest.sessionId(),
                safeRequest.message(),
                safeRequest.currentCartItemIds(),
                history
        );

        ChatbotResponse response = aiClient.post("/ai/chatbot", requestWithHistory, ChatbotResponse.class, AiOperation.CHATBOT)
                .orElseGet(() -> new ChatbotResponse(false, "AI service is temporarily unavailable", List.of()));

        if (response.success()) {
            chatbotHistoryCache.appendAndSave(
                    safeRequest.sessionId(),
                    safeRequest.message(),
                    response.replyText(),
                    history
            );
        }

        return response;
    }

    private ChatbotRequest normalize(ChatbotRequest request) {
        if (request == null) {
            throw new DomainException("Chatbot request is required");
        }

        String sessionId = normalizeOptionalText(request.sessionId());
        if (sessionId == null) {
            throw new DomainException("sessionId is required");
        }

        String message = normalizeOptionalText(request.message());
        if (message == null) {
            throw new DomainException("message is required");
        }

        List<Long> currentCartItemIds = request.currentCartItemIds() == null
                ? List.of()
                : request.currentCartItemIds().stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList();

        return new ChatbotRequest(sessionId, message, currentCartItemIds);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
