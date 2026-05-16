package iuh.fit.se.kitchen.api;

import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.shared.response.PagedResponse;
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

    /** Paginated DONE/CANCELLED history (KDS "Đã xong" tab). */
    @GetMapping("/tasks/paged")
    public ResponseEntity<ApiResponse<PagedResponse<KitchenTaskResponse>>> getCompletedTasksPaged(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(kitchenService.getCompletedTasksPaged(page, size)));
    }

    @PutMapping("/tasks/{id}/start")
    public ResponseEntity<ApiResponse<KitchenTaskResponse>> startTask(
            @PathVariable("id") Long taskId
    ) {
        KitchenTaskResponse response = kitchenService.startTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen task started", response));
    }

    @PutMapping("/tasks/{id}/done")
    public ResponseEntity<ApiResponse<KitchenTaskResponse>> completeTask(
            @PathVariable("id") Long taskId
    ) {
        KitchenTaskResponse response = kitchenService.completeTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen task completed", response));
    }

    @PutMapping("/tasks/{id}/cancel")
    public ResponseEntity<ApiResponse<KitchenTaskResponse>> cancelTask(
            @PathVariable("id") Long taskId
    ) {
        KitchenTaskResponse response = kitchenService.cancelTask(taskId);
        return ResponseEntity.ok(ApiResponse.ok("Kitchen task cancelled", response));
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
