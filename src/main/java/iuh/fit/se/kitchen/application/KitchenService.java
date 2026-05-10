package iuh.fit.se.kitchen.application;

import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.shared.response.PagedResponse;
import java.util.List;

public interface KitchenService {

    List<KitchenTaskResponse> getTasks(KitchenTaskStatus status);

    /** Paged listing for completed/cancelled history views (KDS "Đã xong" tab). */
    PagedResponse<KitchenTaskResponse> getCompletedTasksPaged(int page, int size);

    KitchenTaskResponse startTask(Long taskId);

    KitchenTaskResponse completeTask(Long taskId);

    List<KitchenBatchResponse> getBatches(KitchenBatchStatus status);

    List<KitchenBatchResponse> suggestBatches();

    KitchenBatchResponse acceptBatch(Long batchId);

    KitchenBatchResponse confirmBatch(Long batchId);

    KitchenBatchResponse startBatch(Long batchId);

    KitchenBatchResponse completeBatch(Long batchId);

    List<KitchenTaskResponse> createTasksForOrder(Long orderId, List<Long> orderItemIds);
}
