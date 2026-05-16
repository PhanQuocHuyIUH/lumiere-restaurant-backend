package iuh.fit.se.ai.application;

import iuh.fit.se.ai.client.dto.ChatbotRequest;
import iuh.fit.se.ai.client.dto.ChatbotResponse;

public interface ChatbotService {
    ChatbotResponse chat(ChatbotRequest request);
}
