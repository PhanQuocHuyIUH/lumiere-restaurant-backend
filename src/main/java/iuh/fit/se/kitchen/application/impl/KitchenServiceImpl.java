package iuh.fit.se.kitchen.application.impl;

import iuh.fit.se.kitchen.domain.BatchPerformance;
import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.kitchen.application.KitchenTaskCookData;
import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchItem;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.kitchen.repository.BatchPerformanceRepository;
import iuh.fit.se.kitchen.repository.KitchenBatchItemRepository;
import iuh.fit.se.kitchen.repository.KitchenBatchRepository;
import iuh.fit.se.kitchen.repository.KitchenTaskRepository;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.shared.event.OrderConfirmedEvent;
import iuh.fit.se.shared.event.OrderItemSnapshot;
import iuh.fit.se.ai.AiClient;
import iuh.fit.se.ai.AiOperation;
import iuh.fit.se.ai.client.dto.KitchenBatchingRequest;
import iuh.fit.se.ai.client.dto.KitchenBatchingResponse;
import iuh.fit.se.ai.client.dto.KitchenBatchSuggestion;
import iuh.fit.se.ai.client.dto.KitchenTaskInput;
import iuh.fit.se.shared.event.BatchDoneEvent;
import iuh.fit.se.shared.event.KitchenTaskDoneEvent;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
@Transactional
public class KitchenServiceImpl implements KitchenService {

    private final KitchenTaskRepository kitchenTaskRepository;
    private final KitchenBatchItemRepository kitchenBatchItemRepository;
    private final BatchPerformanceRepository batchPerformanceRepository;
    private final KitchenBatchRepository kitchenBatchRepository;
    private final OrderingService orderingService;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public KitchenServiceImpl(
            KitchenTaskRepository kitchenTaskRepository,
            KitchenBatchItemRepository kitchenBatchItemRepository,
            BatchPerformanceRepository batchPerformanceRepository,
            KitchenBatchRepository kitchenBatchRepository,
            OrderingService orderingService,
            AiClient aiClient,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.kitchenTaskRepository = kitchenTaskRepository;
        this.kitchenBatchItemRepository = kitchenBatchItemRepository;
        this.batchPerformanceRepository = batchPerformanceRepository;
        this.kitchenBatchRepository = kitchenBatchRepository;
        this.orderingService = orderingService;
        this.aiClient = aiClient;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenTaskResponse> getTasks(KitchenTaskStatus status) {
        List<KitchenTask> tasks = status == null
                ? kitchenTaskRepository.findAllByOrderByIdDesc()
                : kitchenTaskRepository.findAllByStatusOrderByIdDesc(status);

        return tasks.stream()
                .map(KitchenTaskResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<KitchenTaskResponse> getCompletedTasksPaged(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<KitchenTask> result = kitchenTaskRepository.findAllByStatusInOrderByIdDesc(
                List.of(KitchenTaskStatus.DONE, KitchenTaskStatus.CANCELLED),
                pageable
        );

        return PagedResponse.from(result.map(KitchenTaskResponse::from));
    }

    @Override
    public KitchenTaskResponse startTask(Long taskId) {
        KitchenTask task = getTaskEntity(taskId);
        task.startCooking();
        kitchenTaskRepository.save(task);

        orderingService.markOrderItemPreparing(task.getOrderItemId());

        KitchenTaskResponse response = KitchenTaskResponse.from(task);
        messagingTemplate.convertAndSend("/topic/kitchen/tasks", response);
        return response;
    }

    @Override
    public KitchenTaskResponse completeTask(Long taskId) {
        KitchenTask task = getTaskEntity(taskId);
        task.complete();
        kitchenTaskRepository.save(task);

        Long orderId = orderingService.markOrderItemDone(task.getOrderItemId());
        eventPublisher.publishEvent(new KitchenTaskDoneEvent(task.getId(), task.getOrderItemId(), orderId));

        KitchenTaskResponse response = KitchenTaskResponse.from(task);
        messagingTemplate.convertAndSend("/topic/kitchen/tasks", response);
        
        // Also notify waiters that a specific item is done
        messagingTemplate.convertAndSend("/topic/waiter/item-done", response);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenBatchResponse> getBatches(KitchenBatchStatus status) {
        List<KitchenBatch> batches = status == null
                ? kitchenBatchRepository.findAllByOrderByCreatedAtDesc()
                : kitchenBatchRepository.findAllByStatusOrderByCreatedAtDesc(status);

        return batches.stream()
                .map(KitchenBatchResponse::from)
                .toList();
    }

    @Override
    public List<KitchenBatchResponse> suggestBatches() {
        List<KitchenTask> createdTasks = kitchenTaskRepository.findAllByStatusOrderByIdDesc(KitchenTaskStatus.CREATED);
        if (createdTasks.isEmpty()) {
            return List.of();
        }

        Set<Long> assignedTaskIds = resolveAssignedTaskIds(createdTasks);
        List<KitchenTask> suggestableTasks = createdTasks.stream()
                .filter(task -> !assignedTaskIds.contains(task.getId()))
                .toList();
        if (suggestableTasks.isEmpty()) {
            return List.of();
        }

        Map<Long, List<KitchenTask>> groupedTasksByMenuItem = groupTasksByMenuItem(suggestableTasks);
        if (groupedTasksByMenuItem.isEmpty()) {
            return List.of();
        }

        List<KitchenBatchResponse> aiResponses = suggestBatchesByAi(suggestableTasks);
        if (!aiResponses.isEmpty()) {
            return aiResponses;
        }

        return suggestBatchesHeuristically(groupedTasksByMenuItem);
    }

    private List<KitchenBatchResponse> suggestBatchesHeuristically(Map<Long, List<KitchenTask>> groupedTasksByMenuItem) {
        List<KitchenBatchResponse> responses = new ArrayList<>();
        groupedTasksByMenuItem.entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> {
                    Long menuItemId = entry.getKey();
                    List<KitchenTask> tasks = entry.getValue();

                    KitchenBatch batch = KitchenBatch.suggestByAi(
                            menuItemId,
                            tasks.size(),
                            estimateAiConfidence(tasks.size()),
                            estimateSavingMinutes(tasks.size()),
                            "Heuristic grouping by menu_item_id over CREATED tasks"
                    );
                    KitchenBatch savedBatch = kitchenBatchRepository.save(batch);

                    List<KitchenBatchItem> batchItems = tasks.stream()
                            .map(task -> KitchenBatchItem.assign(savedBatch.getId(), task.getId()))
                            .toList();
                    kitchenBatchItemRepository.saveAll(batchItems);

                    KitchenBatchResponse response = KitchenBatchResponse.from(savedBatch);
                    responses.add(response);
                    messagingTemplate.convertAndSend("/topic/kitchen/batches", response);
                });

        return responses;
    }

    private List<KitchenBatchResponse> suggestBatchesByAi(List<KitchenTask> suggestableTasks) {
        List<KitchenTaskInput> activeTasks = suggestableTasks.stream()
                .filter(task -> task.getId() != null && task.getMenuItemId() != null)
                .map(this::toKitchenTaskInput)
                .toList();

        if (activeTasks.size() < 2) {
            return List.of();
        }

        KitchenBatchingRequest request = new KitchenBatchingRequest(activeTasks);
        KitchenBatchingResponse aiResponse = aiClient
                .post("/ai/kitchen-batching", request, KitchenBatchingResponse.class, AiOperation.BATCHING)
                .orElse(null);

        if (aiResponse == null || !aiResponse.success() || aiResponse.suggestions() == null || aiResponse.suggestions().isEmpty()) {
            return List.of();
        }

        Map<Long, KitchenTask> taskById = suggestableTasks.stream()
                .filter(task -> task.getId() != null)
                .collect(Collectors.toMap(KitchenTask::getId, task -> task, (left, right) -> left));

        Set<Long> usedTaskIds = new HashSet<>();
        List<KitchenBatchResponse> responses = new ArrayList<>();

        for (KitchenBatchSuggestion suggestion : aiResponse.suggestions()) {
            if (suggestion == null || suggestion.taskIds() == null || suggestion.taskIds().size() < 2) {
                continue;
            }

            List<KitchenTask> tasks = suggestion.taskIds().stream()
                    .filter(taskId -> taskId != null && !usedTaskIds.contains(taskId))
                    .map(taskById::get)
                    .filter(task -> task != null && task.getMenuItemId() != null)
                    .toList();

            if (tasks.size() < 2) {
                continue;
            }

            Long menuItemId = resolveMenuItemId(suggestion, tasks);
            if (menuItemId == null) {
                continue;
            }

            boolean sameMenuItem = tasks.stream().allMatch(task -> menuItemId.equals(task.getMenuItemId()));
            if (!sameMenuItem) {
                continue;
            }

            KitchenBatch batch = KitchenBatch.suggestByAi(
                    menuItemId,
                    tasks.size(),
                    estimateAiConfidence(tasks.size()),
                    normalizeEstimatedSavingMinutes(suggestion.estimatedSavingMinutes()),
                    normalizeBatchReason(suggestion.reason())
            );
            KitchenBatch savedBatch = kitchenBatchRepository.save(batch);

            List<KitchenBatchItem> batchItems = tasks.stream()
                    .map(task -> KitchenBatchItem.assign(savedBatch.getId(), task.getId()))
                    .toList();
            kitchenBatchItemRepository.saveAll(batchItems);

            tasks.stream().map(KitchenTask::getId).forEach(usedTaskIds::add);

            KitchenBatchResponse response = KitchenBatchResponse.from(savedBatch);
            responses.add(response);
            messagingTemplate.convertAndSend("/topic/kitchen/batches", response);
        }

        return responses;
    }

    @Override
    public KitchenBatchResponse acceptBatch(Long batchId) {
        KitchenBatch batch = getBatchEntity(batchId);
        batch.confirm();
        kitchenBatchRepository.save(batch);

        KitchenBatchResponse response = KitchenBatchResponse.from(batch);
        messagingTemplate.convertAndSend("/topic/kitchen/batches", response);
        return response;
    }

    @Override
    public KitchenBatchResponse confirmBatch(Long batchId) {
        return acceptBatch(batchId);
    }

    @Override
    public KitchenBatchResponse startBatch(Long batchId) {
        KitchenBatch batch = getBatchEntity(batchId);
        List<KitchenTask> batchTasks = getBatchTasks(batchId);

        // Cascade: any task still CREATED transitions to COOKING and the matching
        // OrderItem is moved to PREPARING — mirrors what startTask() does individually.
        // Tasks already COOKING/DONE/CANCELLED are left alone.
        List<KitchenTask> started = new ArrayList<>();
        for (KitchenTask task : batchTasks) {
            if (task.getStatus() != KitchenTaskStatus.CREATED) continue;
            task.startCooking();
            orderingService.markOrderItemPreparing(task.getOrderItemId());
            started.add(task);
        }
        if (!started.isEmpty()) {
            kitchenTaskRepository.saveAll(started);
            started.stream()
                    .map(KitchenTaskResponse::from)
                    .forEach(r -> messagingTemplate.convertAndSend("/topic/kitchen/tasks", r));
        }

        batch.start();
        kitchenBatchRepository.save(batch);

        KitchenBatchResponse response = KitchenBatchResponse.from(batch);
        messagingTemplate.convertAndSend("/topic/kitchen/batches", response);
        return response;
    }

    @Override
    public KitchenBatchResponse completeBatch(Long batchId) {
        KitchenBatch batch = getBatchEntity(batchId);
        List<KitchenTask> batchTasks = getBatchTasks(batchId);

        // Cascade: any task still COOKING transitions to DONE so the kitchen can
        // finish the whole batch in one click without first ticking every dish.
        // CANCELLED tasks are skipped; CREATED tasks block completion (kitchen must
        // start them first — completing food that was never cooked is a bug).
        List<KitchenTask> completed = new ArrayList<>();
        for (KitchenTask task : batchTasks) {
            if (task.getStatus() != KitchenTaskStatus.COOKING) continue;
            task.complete();
            Long orderId = orderingService.markOrderItemDone(task.getOrderItemId());
            eventPublisher.publishEvent(new KitchenTaskDoneEvent(task.getId(), task.getOrderItemId(), orderId));
            completed.add(task);
        }
        if (!completed.isEmpty()) {
            kitchenTaskRepository.saveAll(completed);
            completed.stream()
                    .map(KitchenTaskResponse::from)
                    .forEach(r -> {
                        messagingTemplate.convertAndSend("/topic/kitchen/tasks", r);
                        messagingTemplate.convertAndSend("/topic/waiter/item-done", r);
                    });
        }

        ensureBatchTasksCompleted(batchId, batchTasks);

        batch.complete();
        kitchenBatchRepository.save(batch);

        int actualMinutes = calculateActualMinutes(batch, batchTasks);
        int baselineMinutes = calculateBaselineMinutes(batch, batchTasks, actualMinutes);

        BatchPerformance performance = batchPerformanceRepository.findByBatchId(batchId)
                .orElseGet(() -> BatchPerformance.create(batchId, baselineMinutes));
        performance.recordActualMinutes(actualMinutes);
        BatchPerformance savedPerformance = batchPerformanceRepository.saveAndFlush(performance);

        eventPublisher.publishEvent(new BatchDoneEvent(batchId, savedPerformance.getSavingMinutes()));

        KitchenBatchResponse response = KitchenBatchResponse.from(batch);
        messagingTemplate.convertAndSend("/topic/kitchen/batches", response);
        return response;
    }

    @Override
    public List<KitchenTaskResponse> createTasksForOrder(OrderConfirmedEvent event) {
        List<OrderItemSnapshot> snapshots = event.getItems();
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }

        List<Long> orderItemIds = snapshots.stream().map(OrderItemSnapshot::orderItemId).toList();

        List<KitchenTask> existingTasks = kitchenTaskRepository.findAllByOrderItemIdIn(orderItemIds);
        Map<Long, KitchenTask> existingByOrderItemId = new LinkedHashMap<>();
        for (KitchenTask task : existingTasks) {
            existingByOrderItemId.put(task.getOrderItemId(), task);
        }

        List<KitchenTask> tasksToCreate = new ArrayList<>();
        for (OrderItemSnapshot snapshot : snapshots) {
            if (existingByOrderItemId.containsKey(snapshot.orderItemId())) {
                continue;
            }

            tasksToCreate.add(KitchenTask.create(
                    event.getOrderId(),
                    event.getTableId(),
                    snapshot.orderItemId(),
                    snapshot.menuItemId(),
                    snapshot.menuItemName(),
                    snapshot.menuItemImageUrl(),
                    snapshot.quantity(),
                    snapshot.note(),
                    event.getOrderNote(),
                    snapshot.cookTime()
            ));
        }

        List<KitchenTask> createdTasks = List.of();
        if (!tasksToCreate.isEmpty()) {
            try {
                createdTasks = kitchenTaskRepository.saveAll(tasksToCreate);
            } catch (DataIntegrityViolationException ex) {
                createdTasks = kitchenTaskRepository.findAllByOrderItemIdIn(orderItemIds).stream()
                        .filter(task -> !existingByOrderItemId.containsKey(task.getOrderItemId()))
                        .toList();
            }
        }

        List<KitchenTaskResponse> responses = createdTasks.stream()
                .map(KitchenTaskResponse::from)
                .toList();

        if (!responses.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/kitchen/tasks", responses);
        }

        return responses;
    }

    private KitchenTask getTaskEntity(Long taskId) {
        return kitchenTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("KitchenTask", taskId));
    }

    private KitchenBatch getBatchEntity(Long batchId) {
        return kitchenBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("KitchenBatch", batchId));
    }

    private Set<Long> resolveAssignedTaskIds(List<KitchenTask> tasks) {
        if (tasks.isEmpty()) {
            return Set.of();
        }

        List<Long> taskIds = tasks.stream()
                .map(KitchenTask::getId)
                .toList();

        List<KitchenBatchItem> assignedItems = kitchenBatchItemRepository.findAllByKitchenTaskIdIn(taskIds);
        Set<Long> assignedTaskIds = new HashSet<>();
        for (KitchenBatchItem item : assignedItems) {
            assignedTaskIds.add(item.getKitchenTaskId());
        }

        return assignedTaskIds;
    }

    private Map<Long, List<KitchenTask>> groupTasksByMenuItem(List<KitchenTask> tasks) {
        Map<Long, List<KitchenTask>> grouped = new LinkedHashMap<>();
        for (KitchenTask task : tasks) {
            Long menuItemId = task.getMenuItemId();
            if (menuItemId == null) {
                continue;
            }
            grouped.computeIfAbsent(menuItemId, ignored -> new ArrayList<>())
                    .add(task);
        }

        return grouped;
    }

    private KitchenTaskInput toKitchenTaskInput(KitchenTask task) {
        Instant createdAt = task.getCreatedAt() == null ? Instant.now() : task.getCreatedAt();
        int quantity = task.getQuantity() == null || task.getQuantity() < 1 ? 1 : task.getQuantity();
        int cookTimeSeconds = resolveCookTimeSeconds(task.getExpectedCookTime());

        return new KitchenTaskInput(
                task.getId(),
                task.getMenuItemId(),
                quantity,
                createdAt.toString(),
                cookTimeSeconds
        );
    }

    private Long resolveMenuItemId(KitchenBatchSuggestion suggestion, List<KitchenTask> tasks) {
        if (suggestion.menuItemId() != null) {
            return suggestion.menuItemId();
        }
        return tasks.isEmpty() ? null : tasks.get(0).getMenuItemId();
    }

    private String normalizeBatchReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "AI suggested batch";
        }
        return reason.trim();
    }

    private int resolveCookTimeSeconds(Integer expectedCookTimeMinutes) {
        int safeMinutes = expectedCookTimeMinutes == null || expectedCookTimeMinutes < 1 ? 5 : expectedCookTimeMinutes;
        return safeMinutes * 60;
    }

    private int normalizeEstimatedSavingMinutes(double estimatedSavingMinutes) {
        if (Double.isNaN(estimatedSavingMinutes) || Double.isInfinite(estimatedSavingMinutes)) {
            return 0;
        }
        return Math.max(0, (int) Math.round(estimatedSavingMinutes));
    }


    private BigDecimal estimateAiConfidence(int quantity) {
        BigDecimal base = BigDecimal.valueOf(0.55d + (Math.max(0, quantity - 2) * 0.10d));
        BigDecimal bounded = base.min(BigDecimal.valueOf(0.95d));
        return bounded.setScale(2, RoundingMode.HALF_UP);
    }

    private int estimateSavingMinutes(int quantity) {
        return Math.max(1, (quantity - 1) * 2);
    }

    private List<KitchenTask> getBatchTasks(Long batchId) {
        List<KitchenBatchItem> batchItems = kitchenBatchItemRepository.findAllByBatchId(batchId);
        if (batchItems.isEmpty()) {
            throw new DomainException("Batch has no assigned tasks");
        }

        List<Long> taskIds = batchItems.stream()
                .map(KitchenBatchItem::getKitchenTaskId)
                .toList();
        List<KitchenTask> tasks = kitchenTaskRepository.findAllById(taskIds);
        if (tasks.size() != taskIds.size()) {
            throw new DomainException("Batch has invalid task references");
        }
        return tasks;
    }

    private void ensureBatchTasksCompleted(Long batchId, List<KitchenTask> tasks) {
        // A batch is "complete" when every task is terminal (DONE or CANCELLED).
        // After completeBatch's cascade, COOKING tasks become DONE; the only thing
        // that should still block is a CREATED task — kitchen hasn't started it yet,
        // so marking it complete without ever cooking is a data bug.
        boolean allTerminal = tasks.stream().allMatch(task ->
                task.getStatus() == KitchenTaskStatus.DONE
                        || task.getStatus() == KitchenTaskStatus.CANCELLED);
        if (!allTerminal) {
            throw new DomainException("Cannot complete batch " + batchId
                    + ": one or more tasks are still pending. Hãy nhấn 'Bắt đầu nấu' trước rồi mới hoàn thành.");
        }
    }

    private int calculateActualMinutes(KitchenBatch batch, List<KitchenTask> tasks) {
        if (batch.getStartedAt() != null && batch.getCompletedAt() != null) {
            long seconds = Duration.between(batch.getStartedAt(), batch.getCompletedAt()).toSeconds();
            return Math.max(1, (int) Math.ceil(seconds / 60.0d));
        }

        long totalCookSeconds = tasks.stream()
                .map(KitchenTask::getActualCookSeconds)
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();
        if (totalCookSeconds > 0) {
            return Math.max(1, (int) Math.ceil(totalCookSeconds / 60.0d));
        }

        return Math.max(1, tasks.size());
    }

    private int calculateBaselineMinutes(KitchenBatch batch, List<KitchenTask> tasks, int actualMinutes) {
        long totalCookSeconds = tasks.stream()
                .map(KitchenTask::getActualCookSeconds)
                .filter(value -> value != null && value > 0)
                .mapToLong(Integer::longValue)
                .sum();
        int baselineFromTasks = totalCookSeconds > 0
                ? Math.max(1, (int) Math.ceil(totalCookSeconds / 60.0d))
                : actualMinutes;

        Integer estimatedSaving = batch.getEstimatedSavingMinutes();
        if (estimatedSaving != null && estimatedSaving > 0) {
            return Math.max(baselineFromTasks, actualMinutes + estimatedSaving);
        }

        return Math.max(actualMinutes, baselineFromTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenTaskCookData> getRecentCompletedTasksForMenuItem(Long menuItemId, int limit) {
        return kitchenTaskRepository
                .findTop10ByMenuItemIdAndStatusOrderByCompletedAtDesc(menuItemId, KitchenTaskStatus.DONE)
                .stream()
                .limit(limit)
                .map(task -> new KitchenTaskCookData(task.getActualCookSeconds()))
                .toList();
    }

    @Override
    public KitchenTaskResponse cancelTask(Long taskId) {
        KitchenTask task = getTaskEntity(taskId);
        task.cancel();
        kitchenTaskRepository.save(task);

        orderingService.cancelOrderItemByKitchen(task.getOrderId(), task.getOrderItemId());

        KitchenTaskResponse response = KitchenTaskResponse.from(task);
        messagingTemplate.convertAndSend("/topic/kitchen/tasks", response);
        return response;
    }

    @Override
    public void cancelTasksForOrderItems(List<Long> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) return;
        List<KitchenTask> tasks = kitchenTaskRepository.findAllByOrderItemIdIn(orderItemIds);
        List<KitchenTask> toCancel = tasks.stream()
                .filter(t -> t.getStatus() == KitchenTaskStatus.CREATED
                        || t.getStatus() == KitchenTaskStatus.COOKING)
                .toList();
        if (toCancel.isEmpty()) return;
        toCancel.forEach(KitchenTask::cancel);
        kitchenTaskRepository.saveAll(toCancel);
        messagingTemplate.convertAndSend("/topic/kitchen/tasks/cancelled",
                toCancel.stream().map(KitchenTask::getId).toList());
    }

}
