package iuh.fit.se.shared.ai.api;

import iuh.fit.se.shared.ai.AiClient;
import iuh.fit.se.shared.ai.AiOperation;
import iuh.fit.se.shared.ai.api.dto.RetrainJobResponse;
import iuh.fit.se.shared.ai.client.dto.AiJobStatusPayload;
import iuh.fit.se.shared.ai.client.dto.AiRetrainPayload;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Manager API for AI model management.
 *
 * Frontend integration (Phase 4):
 *  1. POST /manager/ai/retrain        → receive HTTP 202 + { jobId, status:"PROCESSING" }
 *  2. Poll GET /manager/ai/jobs/{jobId} every 5s until status = COMPLETED or FAILED
 *  3. Show spinner while PROCESSING; toast on completion
 */
@RestController
@RequestMapping("/manager/ai")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerAiController {

    private final AiClient aiClient;

    public ManagerAiController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @PostMapping("/retrain")
    public ResponseEntity<ApiResponse<RetrainJobResponse>> triggerRetrain() {
        Optional<AiRetrainPayload> response = aiClient.post(
                "/ai/retrain", Map.of(), AiRetrainPayload.class, AiOperation.RETRAIN
        );

        if (response.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("AI service is unavailable — retrain could not be started"));
        }

        AiRetrainPayload payload = response.get();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Retraining job started", new RetrainJobResponse(payload.jobId(), "PROCESSING")));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ApiResponse<AiJobStatusPayload>> getJobStatus(@PathVariable String jobId) {
        Optional<AiJobStatusPayload> response = aiClient.get(
                "/ai/jobs/" + jobId, AiJobStatusPayload.class, AiOperation.JOB_STATUS
        );

        return response
                .map(status -> ResponseEntity.ok(ApiResponse.ok("Job status retrieved", status)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error("AI service is unavailable")));
    }
}
