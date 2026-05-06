package iuh.fit.se.kitchen.application.impl;

import iuh.fit.se.kitchen.domain.BatchPerformance;
import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchItem;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.kitchen.infrastructure.BatchPerformanceRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenBatchItemRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenBatchRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenTaskRepository;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.infrastructure.MenuItemRepository;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.infrastructure.OrderRepository;
import iuh.fit.se.ordering.infrastructure.OrderItemRepository;
import iuh.fit.se.shared.ai.AiClient;
import iuh.fit.se.shared.ai.AiOperation;
import iuh.fit.se.shared.ai.client.dto.KitchenBatchingRequest;
import iuh.fit.se.shared.ai.client.dto.KitchenBatchingResponse;
import iuh.fit.se.shared.ai.client.dto.KitchenBatchSuggestion;
import iuh.fit.se.shared.ai.client.dto.KitchenTaskInput;
import iuh.fit.se.shared.event.BatchDoneEvent;
import iuh.fit.se.shared.event.KitchenTaskDoneEvent;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
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
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderingService orderingService;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    public KitchenServiceImpl(
            KitchenTaskRepository kitchenTaskRepository,
            KitchenBatchItemRepository kitchenBatchItemRepository,
            BatchPerformanceRepository batchPerformanceRepository,
            KitchenBatchRepository kitchenBatchRepository,
                OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
                MenuItemRepository menuItemRepository,
            OrderingService orderingService,
            AiClient aiClient,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.kitchenTaskRepository = kitchenTaskRepository;
        this.kitchenBatchItemRepository = kitchenBatchItemRepository;
        this.batchPerformanceRepository = batchPerformanceRepository;
        this.kitchenBatchRepository = kitchenBatchRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
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
    public List<KitchenTaskResponse> createTasksForOrder(Long orderId, List<Long> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return List.of();
        }

        Order order = getOrderEntity(orderId);
        List<OrderItem> orderItems = orderItemRepository.findAllById(orderItemIds);
        Map<Long, OrderItem> orderItemsById = new LinkedHashMap<>();
        for (OrderItem orderItem : orderItems) {
            orderItemsById.put(orderItem.getId(), orderItem);
        }

        List<Long> menuItemIds = orderItems.stream()
                .map(OrderItem::getMenuItemId)
                .filter(menuItemId -> menuItemId != null)
                .distinct()
                .toList();
        List<MenuItem> menuItems = menuItemRepository.findAllById(menuItemIds);
        Map<Long, MenuItem> menuItemsById = new LinkedHashMap<>();
        for (MenuItem menuItem : menuItems) {
            menuItemsById.put(menuItem.getId(), menuItem);
        }

        List<KitchenTask> existingTasks = kitchenTaskRepository.findAllByOrderItemIdIn(orderItemIds);
        Map<Long, KitchenTask> existingByOrderItemId = new LinkedHashMap<>();
        for (KitchenTask task : existingTasks) {
            existingByOrderItemId.put(task.getOrderItemId(), task);
        }

        List<KitchenTask> tasksToCreate = new ArrayList<>();
        for (Long orderItemId : orderItemIds) {
            if (existingByOrderItemId.containsKey(orderItemId)) {
                continue;
            }

            OrderItem orderItem = orderItemsById.get(orderItemId);
            if (orderItem == null || orderItem.getMenuItemId() == null) {
                continue;
            }

            MenuItem menuItem = menuItemsById.get(orderItem.getMenuItemId());
            if (menuItem == null) {
                continue;
            }

            tasksToCreate.add(KitchenTask.create(
                    order.getId(),
                    order.getTableId(),
                    orderItem.getId(),
                    menuItem.getId(),
                    menuItem.getName(),
                    menuItem.getImageUrl(),
                    orderItem.getQuantity(),
                    orderItem.getNote(),
                    order.getNote(),
                    menuItem.getCookTime()
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

    private Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
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
        boolean allDone = tasks.stream().allMatch(task -> task.getStatus() == KitchenTaskStatus.DONE);
        if (!allDone) {
            throw new DomainException("Cannot complete batch " + batchId + " when not all tasks are DONE");
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

}
