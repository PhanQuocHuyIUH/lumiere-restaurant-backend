package iuh.fit.se.kitchen.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.kitchen.api.dto.KitchenBatchResponse;
import iuh.fit.se.kitchen.api.dto.KitchenTaskResponse;
import iuh.fit.se.kitchen.application.KitchenService;
import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenIdempotencyKey;
import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.kitchen.infrastructure.KitchenBatchRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenIdempotencyKeyRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenTaskRepository;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.shared.event.KitchenTaskDoneEvent;
import iuh.fit.se.shared.exception.IdempotencyConflictException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.util.IdempotencyUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

@Service
@Transactional
public class KitchenServiceImpl implements KitchenService {

    private static final String IDEM_MODULE = "kitchen";
    private static final String OP_START_TASK = "START_TASK";
    private static final String OP_COMPLETE_TASK = "COMPLETE_TASK";

    private final KitchenTaskRepository kitchenTaskRepository;
    private final KitchenBatchRepository kitchenBatchRepository;
    private final KitchenIdempotencyKeyRepository idempotencyKeyRepository;
    private final OrderingService orderingService;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public KitchenServiceImpl(
            KitchenTaskRepository kitchenTaskRepository,
            KitchenBatchRepository kitchenBatchRepository,
            KitchenIdempotencyKeyRepository idempotencyKeyRepository,
            OrderingService orderingService,
            ApplicationEventPublisher eventPublisher,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper
    ) {
        this.kitchenTaskRepository = kitchenTaskRepository;
        this.kitchenBatchRepository = kitchenBatchRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.orderingService = orderingService;
        this.eventPublisher = eventPublisher;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
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
    public KitchenTaskResponse startTask(Long taskId, String idempotencyKey) {
        return executeIdempotent(idempotencyKey, OP_START_TASK, HttpStatus.OK.value(), KitchenTaskResponse.class, () -> {
            KitchenTask task = getTaskEntity(taskId);
            task.startCooking();
            kitchenTaskRepository.save(task);

            orderingService.markOrderItemPreparing(task.getOrderItemId());

            KitchenTaskResponse response = KitchenTaskResponse.from(task);
            messagingTemplate.convertAndSend("/topic/kitchen/tasks", response);
            return response;
        });
    }

    @Override
    public KitchenTaskResponse completeTask(Long taskId, String idempotencyKey) {
        return executeIdempotent(idempotencyKey, OP_COMPLETE_TASK, HttpStatus.OK.value(), KitchenTaskResponse.class, () -> {
            KitchenTask task = getTaskEntity(taskId);
            task.complete();
            kitchenTaskRepository.save(task);

            Long orderId = orderingService.markOrderItemDone(task.getOrderItemId());
            eventPublisher.publishEvent(new KitchenTaskDoneEvent(task.getId(), task.getOrderItemId(), orderId));

            KitchenTaskResponse response = KitchenTaskResponse.from(task);
            messagingTemplate.convertAndSend("/topic/kitchen/tasks", response);
            return response;
        });
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
    public KitchenBatchResponse confirmBatch(Long batchId) {
        KitchenBatch batch = getBatchEntity(batchId);
        batch.confirm();
        kitchenBatchRepository.save(batch);

        KitchenBatchResponse response = KitchenBatchResponse.from(batch);
        messagingTemplate.convertAndSend("/topic/kitchen/batches", response);
        return response;
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
    public List<KitchenTaskResponse> createTasksForOrder(Long orderId, List<Long> orderItemIds) {
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            return List.of();
        }

        List<KitchenTask> existingTasks = kitchenTaskRepository.findAllByOrderItemIdIn(orderItemIds);
        Map<Long, KitchenTask> existingByOrderItemId = new LinkedHashMap<>();
        for (KitchenTask task : existingTasks) {
            existingByOrderItemId.put(task.getOrderItemId(), task);
        }

        List<KitchenTask> tasksToCreate = new ArrayList<>();
        for (Long orderItemId : orderItemIds) {
            if (!existingByOrderItemId.containsKey(orderItemId)) {
                tasksToCreate.add(KitchenTask.create(orderItemId));
            }
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

    private <T> T executeIdempotent(
            String rawKey,
            String operation,
            int responseStatus,
            Class<T> responseClass,
            Supplier<T> action
    ) {
        String normalizedKey = IdempotencyUtil.normalizeKey(rawKey);
        Optional<KitchenIdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findByModuleAndOperationAndIdemKey(
                IDEM_MODULE,
                operation,
                normalizedKey
        );
        if (existingKeyOpt.isPresent()) {
            KitchenIdempotencyKey existing = existingKeyOpt.get();
            if (existing.isExpired(Instant.now())) {
                idempotencyKeyRepository.delete(existing);
                idempotencyKeyRepository.flush();
            } else if (existing.hasResponseBody()) {
                return IdempotencyUtil.fromJsonMap(objectMapper, existing.getResponseBody(), responseClass);
            } else {
                throw new IdempotencyConflictException(normalizedKey);
            }
        }

        KitchenIdempotencyKey pendingKey = KitchenIdempotencyKey.reserve(
                IDEM_MODULE,
                operation,
                normalizedKey,
                IdempotencyUtil.defaultExpiry()
        );
        idempotencyKeyRepository.save(pendingKey);

        T response = action.get();

        pendingKey.markCompleted(responseStatus, IdempotencyUtil.toJsonMap(objectMapper, response));
        idempotencyKeyRepository.save(pendingKey);

        return response;
    }

}
