package iuh.fit.se.ordering.api;

import iuh.fit.se.ordering.api.dto.ChatbotMessageRequest;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.shared.ai.client.dto.ChatbotResponse;
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

    private final OrderingService orderingService;

    public ChatbotController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatbotResponse>> chat(
            @Valid @RequestBody ChatbotMessageRequest request
    ) {
        ChatbotResponse response = orderingService.chatbot(request.toAiRequest());
        return ResponseEntity.ok(ApiResponse.ok("Chatbot response generated", response));
    }
}
