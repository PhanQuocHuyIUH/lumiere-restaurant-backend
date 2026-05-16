package iuh.fit.se.ai.api;

import iuh.fit.se.ai.AiRuntimeState;
import iuh.fit.se.ai.api.dto.AiRuntimeStatusResponse;
import iuh.fit.se.ai.config.AiProperties;
import iuh.fit.se.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
public class AiInternalController {

    private final AiRuntimeState aiRuntimeState;
    private final AiProperties aiProperties;

    public AiInternalController(AiRuntimeState aiRuntimeState, AiProperties aiProperties) {
        this.aiRuntimeState = aiRuntimeState;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AiRuntimeStatusResponse>> getStatus() {
        return ResponseEntity.ok(ApiResponse.ok(toStatusResponse()));
    }

    @PutMapping("/runtime-enabled")
    public ResponseEntity<ApiResponse<AiRuntimeStatusResponse>> updateRuntimeEnabled(@RequestParam("enabled") boolean enabled) {
        aiRuntimeState.setRuntimeEnabled(enabled);
        return ResponseEntity.ok(ApiResponse.ok("AI runtime switch updated", toStatusResponse()));
    }

    private AiRuntimeStatusResponse toStatusResponse() {
        return new AiRuntimeStatusResponse(
                aiRuntimeState.isConfiguredEnabled(),
                aiRuntimeState.isRuntimeEnabled(),
                aiRuntimeState.isHealthy(),
                aiRuntimeState.isAvailable(),
                aiRuntimeState.getLastHealthCheckedAt(),
                aiRuntimeState.getHealthMessage(),
                aiProperties.getBaseUrl()
        );
    }
}
