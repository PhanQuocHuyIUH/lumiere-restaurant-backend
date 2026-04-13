package iuh.fit.se.kitchen.api;

import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<KitchenTaskResponse>>> getTasks(
            @RequestParam(value = "status", required = false) KitchenTaskStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.getTasks(status)));
    }

    @PutMapping("/tasks/{id}/start")
    public ResponseEntity<ApiResponse<KitchenTaskResponse>> startTask(
            @PathVariable("id") Long taskId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey
    ) {
        KitchenTaskResponse response = kitchenService.startTask(taskId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen task started", response));
    }

    @PutMapping("/tasks/{id}/done")
    public ResponseEntity<ApiResponse<KitchenTaskResponse>> completeTask(
            @PathVariable("id") Long taskId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey
    ) {
        KitchenTaskResponse response = kitchenService.completeTask(taskId, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen task completed", response));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<KitchenBatchResponse>>> getBatches(
            @RequestParam(value = "status", required = false) KitchenBatchStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.getBatches(status)));
    }

    @PutMapping("/batches/{id}/confirm")
    public ResponseEntity<ApiResponse<KitchenBatchResponse>> confirmBatch(@PathVariable("id") Long batchId) {
        KitchenBatchResponse response = kitchenService.confirmBatch(batchId);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen batch confirmed", response));
    }

    @PutMapping("/batches/{id}/start")
    public ResponseEntity<ApiResponse<KitchenBatchResponse>> startBatch(@PathVariable("id") Long batchId) {
        KitchenBatchResponse response = kitchenService.startBatch(batchId);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen batch started", response));
    }
}
