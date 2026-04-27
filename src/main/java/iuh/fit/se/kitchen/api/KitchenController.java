package iuh.fit.se.kitchen.api;

import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/batches/suggest")
    public ResponseEntity<ApiResponse<List<KitchenBatchResponse>>> suggestBatches() {
        return ResponseEntity.ok(ApiResponse.ok("Kitchen batch suggestions generated", kitchenService.suggestBatches()));
    }

    @PutMapping("/batches/{id}/accept")
    public ResponseEntity<ApiResponse<KitchenBatchResponse>> acceptBatch(@PathVariable("id") Long batchId) {
        KitchenBatchResponse response = kitchenService.acceptBatch(batchId);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen batch accepted", response));
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

    @PutMapping("/batches/{id}/done")
    public ResponseEntity<ApiResponse<KitchenBatchResponse>> completeBatch(@PathVariable("id") Long batchId) {
        KitchenBatchResponse response = kitchenService.completeBatch(batchId);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen batch completed", response));
    }
}
