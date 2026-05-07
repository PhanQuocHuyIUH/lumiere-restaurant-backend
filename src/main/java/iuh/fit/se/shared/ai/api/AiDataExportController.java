package iuh.fit.se.shared.ai.api;

import iuh.fit.se.analytics.infrastructure.OrderEventRepository;
import iuh.fit.se.billing.domain.PaymentStatus;
import iuh.fit.se.billing.infrastructure.PaymentRepository;
import iuh.fit.se.menu.domain.MenuCategory;
import iuh.fit.se.menu.domain.MenuItem;
import iuh.fit.se.menu.infrastructure.MenuCategoryRepository;
import iuh.fit.se.menu.infrastructure.MenuItemRepository;
import iuh.fit.se.table.infrastructure.RestaurantTableRepository;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import iuh.fit.se.kitchen.infrastructure.BatchPerformanceRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenBatchRepository;
import iuh.fit.se.kitchen.infrastructure.KitchenTaskRepository;
import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderRevision;
import iuh.fit.se.ordering.domain.OrderItemStatus;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.ordering.domain.RevisionSource;
import iuh.fit.se.ordering.infrastructure.OrderItemRepository;
import iuh.fit.se.ordering.infrastructure.OrderRevisionRepository;
import iuh.fit.se.ordering.infrastructure.OrderRepository;
import iuh.fit.se.shared.ai.api.dto.AiExportPageResponse;
import iuh.fit.se.shared.ai.api.dto.AnalyticsEventExportRow;
import iuh.fit.se.shared.ai.api.dto.BatchPerformanceExportRow;
import iuh.fit.se.shared.ai.api.dto.ExportOrderRevisionContext;
import iuh.fit.se.shared.ai.api.dto.ExportTableContext;
import iuh.fit.se.shared.ai.api.dto.KitchenBatchExportRow;
import iuh.fit.se.shared.ai.api.dto.KitchenTaskExportRow;
import iuh.fit.se.shared.ai.api.dto.MenuItemExportRow;
import iuh.fit.se.shared.ai.api.dto.OrderExportRow;
import iuh.fit.se.shared.ai.api.dto.OrderItemExportRow;
import iuh.fit.se.shared.ai.api.dto.PaymentExportRow;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.response.ApiResponse;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/export")
public class AiDataExportController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 100;
    private static final int MAX_PAGE_SIZE = 500;
    private static final Instant DEFAULT_FROM_TIME = Instant.EPOCH;
    private static final Instant DEFAULT_TO_TIME = Instant.parse("3000-01-01T00:00:00Z");

    private final OrderRepository orderRepository;
    private final OrderRevisionRepository orderRevisionRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventRepository orderEventRepository;
    private final KitchenTaskRepository kitchenTaskRepository;
    private final KitchenBatchRepository kitchenBatchRepository;
    private final BatchPerformanceRepository batchPerformanceRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public AiDataExportController(
            OrderRepository orderRepository,
            OrderRevisionRepository orderRevisionRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            OrderEventRepository orderEventRepository,
            KitchenTaskRepository kitchenTaskRepository,
            KitchenBatchRepository kitchenBatchRepository,
            BatchPerformanceRepository batchPerformanceRepository,
            RestaurantTableRepository restaurantTableRepository,
            MenuItemRepository menuItemRepository,
            MenuCategoryRepository menuCategoryRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderRevisionRepository = orderRevisionRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.orderEventRepository = orderEventRepository;
        this.kitchenTaskRepository = kitchenTaskRepository;
        this.kitchenBatchRepository = kitchenBatchRepository;
        this.batchPerformanceRepository = batchPerformanceRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    @GetMapping("/menu-items")
    public ResponseEntity<ApiResponse<AiExportPageResponse<MenuItemExportRow>>> exportMenuItems(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<MenuItem> menuItemPage = menuItemRepository.findAllByDeletedAtIsNull(pageable);

        Set<Long> categoryIds = menuItemPage.getContent().stream()
                .map(MenuItem::getCategoryId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, String> categoryNameById = categoryIds.isEmpty() ? Map.of() :
                menuCategoryRepository.findAllById(categoryIds).stream()
                        .collect(Collectors.toMap(MenuCategory::getId, MenuCategory::getName, (a, b) -> a));

        Page<MenuItemExportRow> result = menuItemPage.map(item ->
                MenuItemExportRow.from(item, categoryNameById.get(item.getCategoryId()))
        );

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<AiExportPageResponse<OrderExportRow>>> exportOrders(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(value = "status", required = false) OrderStatus status
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Order> orderPage = orderRepository.searchForAiExport(
            toFromInstant(fromDate),
            toToExclusiveInstant(toDate),
            status != null,
            fallbackEnum(status, OrderStatus.CREATED),
            pageable
        );

        Map<Long, ExportOrderRevisionContext> revisionContextByOrderId = loadLatestRevisionContextByOrderId(
            orderPage.getContent()
        );
        Map<Long, ExportTableContext> tableContextByTableId = loadTableContextByOrders(orderPage.getContent());

        Page<OrderExportRow> result = orderPage.map(order -> OrderExportRow.from(
            order,
            revisionContextByOrderId.get(order.getId()),
            tableContextByTableId.get(order.getTableId())
        ));

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    @GetMapping("/order-items")
    public ResponseEntity<ApiResponse<AiExportPageResponse<OrderItemExportRow>>> exportOrderItems(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(value = "status", required = false) OrderItemStatus status
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<OrderItem> orderItemPage = orderItemRepository.searchForAiExport(
            toFromInstant(fromDate),
            toToExclusiveInstant(toDate),
            status != null,
            fallbackEnum(status, OrderItemStatus.PENDING),
            pageable
        );

        Map<Long, KitchenTask> kitchenTaskByOrderItemId = loadKitchenTaskByOrderItemId(orderItemPage.getContent());
        Map<Long, OrderRevision> revisionById = loadOrderRevisionByRevisionId(orderItemPage.getContent());
        Map<Long, Order> orderById = loadOrderByOrderId(revisionById.values());
        Map<Long, ExportTableContext> tableContextByTableId = loadTableContextByOrders(orderById.values());

        Page<OrderItemExportRow> result = orderItemPage.map(orderItem -> {
            OrderRevision revision = revisionById.get(orderItem.getRevisionId());
            ExportOrderRevisionContext revisionContext = revision == null
                ? null
                : ExportOrderRevisionContext.from(revision);

            Order order = revision == null ? null : orderById.get(revision.getOrderId());
            ExportTableContext tableContext = order == null ? null : tableContextByTableId.get(order.getTableId());
            KitchenTask kitchenTask = kitchenTaskByOrderItemId.get(orderItem.getId());

            return OrderItemExportRow.from(orderItem, order, revisionContext, tableContext, kitchenTask);
        });

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<AiExportPageResponse<PaymentExportRow>>> exportPayments(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(value = "status", required = false) PaymentStatus status
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<PaymentExportRow> result = paymentRepository.searchForAiExport(
                toFromInstant(fromDate),
                toToExclusiveInstant(toDate),
                status != null,
                fallbackEnum(status, PaymentStatus.PENDING),
                        pageable
                )
                .map(PaymentExportRow::from);

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    @GetMapping("/analytics-events")
    public ResponseEntity<ApiResponse<AiExportPageResponse<AnalyticsEventExportRow>>> exportAnalyticsEvents(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(value = "eventType", required = false) String eventType
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<AnalyticsEventExportRow> result = orderEventRepository.searchForAiExport(
                toFromInstant(fromDate),
                toToExclusiveInstant(toDate),
                eventType != null && !eventType.isBlank(),
                normalizeKeyword(eventType),
                        pageable
                )
                .map(AnalyticsEventExportRow::from);

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    @GetMapping("/kitchen-tasks")
    public ResponseEntity<ApiResponse<AiExportPageResponse<KitchenTaskExportRow>>> exportKitchenTasks(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(value = "status", required = false) KitchenTaskStatus status
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<KitchenTaskExportRow> result = kitchenTaskRepository.searchForAiExport(
                toFromInstant(fromDate),
                toToExclusiveInstant(toDate),
                status != null,
                fallbackEnum(status, KitchenTaskStatus.CREATED),
                        pageable
                )
                .map(KitchenTaskExportRow::from);

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    @GetMapping("/kitchen-batches")
    public ResponseEntity<ApiResponse<AiExportPageResponse<KitchenBatchExportRow>>> exportKitchenBatches(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,
            @RequestParam(value = "status", required = false) KitchenBatchStatus status
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<KitchenBatchExportRow> result = kitchenBatchRepository.searchForAiExport(
                toFromInstant(fromDate),
                toToExclusiveInstant(toDate),
                status != null,
                fallbackEnum(status, KitchenBatchStatus.SUGGESTED),
                        pageable
                )
                .map(KitchenBatchExportRow::from);

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    @GetMapping("/batch-performances")
    public ResponseEntity<ApiResponse<AiExportPageResponse<BatchPerformanceExportRow>>> exportBatchPerformances(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "fromDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,
            @RequestParam(value = "toDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        validateDateRange(fromDate, toDate);
        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "recordedAt", "id"));
        Page<BatchPerformanceExportRow> result = batchPerformanceRepository.searchForAiExport(
                toFromInstant(fromDate),
                toToExclusiveInstant(toDate),
                        pageable
                )
                .map(BatchPerformanceExportRow::from);

        return ResponseEntity.ok(ApiResponse.ok(AiExportPageResponse.from(result)));
    }

    private Map<Long, ExportOrderRevisionContext> loadLatestRevisionContextByOrderId(Collection<Order> orders) {
        Set<Long> orderIds = orders.stream()
                .map(Order::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (orderIds.isEmpty()) {
            return Map.of();
        }

        return orderRevisionRepository.findLatestContextByOrderIds(orderIds).stream()
                .map(this::toRevisionContext)
                .collect(Collectors.toMap(ExportOrderRevisionContext::orderId, Function.identity(), (left, right) -> left));
    }

    private ExportOrderRevisionContext toRevisionContext(OrderRevisionRepository.LatestOrderRevisionProjection projection) {
        return new ExportOrderRevisionContext(
                projection.getOrderId(),
                projection.getRevisionNumber(),
                parseRevisionSource(projection.getRevisionSource()),
                projection.getQrSessionId(),
                projection.getCreatedByStaffId(),
                projection.getCreatedAt()
        );
    }

    private Map<Long, OrderRevision> loadOrderRevisionByRevisionId(Collection<OrderItem> orderItems) {
        Set<Long> revisionIds = orderItems.stream()
                .map(OrderItem::getRevisionId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (revisionIds.isEmpty()) {
            return Map.of();
        }

        return orderRevisionRepository.findAllById(revisionIds).stream()
                .collect(Collectors.toMap(OrderRevision::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Order> loadOrderByOrderId(Collection<OrderRevision> revisions) {
        Set<Long> orderIds = revisions.stream()
                .map(OrderRevision::getOrderId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (orderIds.isEmpty()) {
            return Map.of();
        }

        return orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, KitchenTask> loadKitchenTaskByOrderItemId(Collection<OrderItem> orderItems) {
        Set<Long> orderItemIds = orderItems.stream()
                .map(OrderItem::getId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (orderItemIds.isEmpty()) {
            return Map.of();
        }

        return kitchenTaskRepository.findAllByOrderItemIdIn(orderItemIds).stream()
                .collect(Collectors.toMap(KitchenTask::getOrderItemId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, ExportTableContext> loadTableContextByOrders(Collection<Order> orders) {
        Set<Long> tableIds = orders.stream()
                .map(Order::getTableId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        return loadTableContextByTableIds(tableIds);
    }

    private Map<Long, ExportTableContext> loadTableContextByTableIds(Collection<Long> tableIds) {
        Set<Long> normalizedTableIds = tableIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        if (normalizedTableIds.isEmpty()) {
            return Map.of();
        }

        return restaurantTableRepository.findAllByIdInAndDeletedAtIsNull(normalizedTableIds).stream()
                .map(ExportTableContext::from)
                .collect(Collectors.toMap(ExportTableContext::tableId, Function.identity(), (left, right) -> left));
    }

    private Pageable buildPageable(Integer page, Integer size, Sort sort) {
        int normalizedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int requestedSize = size == null ? DEFAULT_SIZE : size;
        int normalizedSize = requestedSize < 1 ? DEFAULT_SIZE : Math.min(requestedSize, MAX_PAGE_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, sort);
    }

    private Instant toFromInstant(LocalDate fromDate) {
        if (fromDate == null) {
            return DEFAULT_FROM_TIME;
        }
        return fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant toToExclusiveInstant(LocalDate toDate) {
        if (toDate == null) {
            return DEFAULT_TO_TIME;
        }
        return toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new DomainException("fromDate must be before or equal to toDate");
        }
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private <E extends Enum<E>> E fallbackEnum(E value, E fallback) {
        return value == null ? fallback : value;
    }

    private RevisionSource parseRevisionSource(String revisionSource) {
        if (revisionSource == null || revisionSource.isBlank()) {
            return null;
        }

        try {
            return RevisionSource.valueOf(revisionSource.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
