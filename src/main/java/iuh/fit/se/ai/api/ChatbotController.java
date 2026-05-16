package iuh.fit.se.ai.api;

import iuh.fit.se.ai.api.dto.ChatbotMessageRequest;
import iuh.fit.se.ai.application.ChatbotService;
import iuh.fit.se.ai.client.dto.ChatbotResponse;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatbotResponse>> chat(
            @Valid @RequestBody ChatbotMessageRequest request
    ) {
        ChatbotResponse response = chatbotService.chat(request.toAiRequest());
        return ResponseEntity.ok(ApiResponse.ok("Chatbot response generated", response));
    }
}
