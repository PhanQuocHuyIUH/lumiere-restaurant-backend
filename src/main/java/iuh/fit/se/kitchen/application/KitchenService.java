package iuh.fit.se.kitchen.application;

import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import java.util.List;

public interface KitchenService {

    List<KitchenTaskResponse> getTasks(KitchenTaskStatus status);

    KitchenTaskResponse startTask(Long taskId, String idempotencyKey);

    KitchenTaskResponse completeTask(Long taskId, String idempotencyKey);

    List<KitchenBatchResponse> getBatches(KitchenBatchStatus status);

    KitchenBatchResponse confirmBatch(Long batchId);

    KitchenBatchResponse startBatch(Long batchId);

    List<KitchenTaskResponse> createTasksForOrder(Long orderId, List<Long> orderItemIds);
}
