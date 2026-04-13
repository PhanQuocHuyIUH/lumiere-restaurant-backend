package iuh.fit.se.ordering.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.catalog.application.CatalogService;
import iuh.fit.se.catalog.application.MenuItemDTO;
import iuh.fit.se.catalog.application.TableDTO;
import iuh.fit.se.ordering.api.dto.AddRevisionRequest;
import iuh.fit.se.ordering.api.dto.CreateOrderRequest;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.ordering.domain.IdempotencyKey;
import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderRevision;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.ordering.domain.RevisionSource;
import iuh.fit.se.ordering.infrastructure.IdempotencyKeyRepository;
import iuh.fit.se.ordering.infrastructure.OrderItemRepository;
import iuh.fit.se.ordering.infrastructure.OrderRepository;
import iuh.fit.se.ordering.infrastructure.OrderRevisionRepository;
import iuh.fit.se.shared.event.OrderCancelledEvent;
import iuh.fit.se.shared.event.OrderConfirmedEvent;
import iuh.fit.se.shared.event.OrderCreatedEvent;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.IdempotencyConflictException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.security.JwtPrincipal;
import iuh.fit.se.shared.util.IdempotencyUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderingServiceImpl implements OrderingService {

    private static final String MODULE = "ordering";
    private static final String OP_CREATE_ORDER = "CREATE_ORDER";
    private static final String OP_ADD_REVISION = "ADD_REVISION";
    private static final String OP_CONFIRM_ORDER = "CONFIRM_ORDER";

    private static final Set<String> STAFF_ORDER_ROLES = Set.of("ROLE_WAITER", "ROLE_MANAGER");
    private static final Set<String> STAFF_CONFIRM_ROLES = Set.of("ROLE_WAITER", "ROLE_CASHIER", "ROLE_MANAGER");

    private static final Set<OrderStatus> REVISION_ALLOWED_STATUSES = Set.of(
            OrderStatus.CREATED,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.SERVED
    );

    private static final Set<OrderStatus> ACTIVE_ORDER_STATUSES = Set.of(
            OrderStatus.CREATED,
            OrderStatus.CONFIRMED,
            OrderStatus.PREPARING,
            OrderStatus.READY,
            OrderStatus.SERVED
    );

    private final OrderRepository orderRepository;
    private final OrderRevisionRepository orderRevisionRepository;
    private final OrderItemRepository orderItemRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final CatalogService catalogService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OrderingServiceImpl(
            OrderRepository orderRepository,
            OrderRevisionRepository orderRevisionRepository,
            OrderItemRepository orderItemRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            CatalogService catalogService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderRevisionRepository = orderRevisionRepository;
        this.orderItemRepository = orderItemRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.catalogService = catalogService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey, String qrSessionId) {
        return executeIdempotent(idempotencyKey, OP_CREATE_ORDER, HttpStatus.CREATED.value(), () -> {
            RevisionActor actor = resolveActorForCreate(qrSessionId, request.tableCode());
            TableDTO table = validateTableCanReceiveOrders(request.tableCode());
            Optional<Order> activeOrderOpt = orderRepository.findTopByTableIdAndStatusInOrderByCreatedAtDesc(
                    table.id(),
                    ACTIVE_ORDER_STATUSES
            );

            if (activeOrderOpt.isPresent()) {
                return createRevisionForExistingOrder(activeOrderOpt.get(), request, actor);
            }

            Map<Long, MenuItemDTO> menuItems = resolveAvailableMenuItems(request.items());

            Order order = Order.builder()
                    .tableId(table.id())
                    .note(normalizeOptionalText(request.note()))
                    .splitBillAllowed(Boolean.TRUE.equals(request.splitBillAllowed()))
                    .build();
            order = orderRepository.save(order);

                OrderRevision revision = OrderRevision.create(
                    order.getId(),
                    1,
                    actor.source(),
                    actor.staffId(),
                    actor.qrSessionId()
                );
            revision = orderRevisionRepository.save(revision);

            List<OrderItem> savedItems = orderItemRepository.saveAll(
                    buildOrderItemsForCreate(revision.getId(), request.items(), menuItems)
            );

            refreshOrderTotal(order, revision.getId());

            eventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), order.getTableId()));
            return OrderResponse.from(order, revision.getRevisionNumber(), savedItems);
        });
    }

    @Override
    public OrderResponse addRevision(Long orderId, AddRevisionRequest request, String idempotencyKey, String qrSessionId) {
        return executeIdempotent(idempotencyKey, OP_ADD_REVISION, HttpStatus.OK.value(), () -> {
            Order order = getOrderEntity(orderId);
            RevisionActor actor = resolveActorForRevision(qrSessionId, order);
            ensureCanAddRevision(order);

            Map<Long, MenuItemDTO> menuItems = resolveAvailableMenuItemsForRevision(request.items());
            int nextRevisionNumber = orderRevisionRepository.findTopByOrderIdOrderByRevisionNumberDesc(orderId)
                    .map(revision -> revision.getRevisionNumber() + 1)
                    .orElse(1);

                OrderRevision revision = OrderRevision.create(
                    order.getId(),
                    nextRevisionNumber,
                    actor.source(),
                    actor.staffId(),
                    actor.qrSessionId()
                );
            revision = orderRevisionRepository.save(revision);

            List<OrderItem> savedItems = orderItemRepository.saveAll(
                    buildOrderItemsForRevision(revision.getId(), request.items(), menuItems)
            );

            if (request.note() != null && !request.note().isBlank()) {
                order.updateNote(request.note().trim());
            }

            if (order.getStatus() == OrderStatus.SERVED) {
                order.reopenForAdditionalItems();
                orderRepository.save(order);

                List<Long> orderItemIds = savedItems.stream()
                        .map(OrderItem::getId)
                        .toList();
                eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), orderItemIds));
            }

            refreshOrderTotal(order, revision.getId());
            return OrderResponse.from(order, revision.getRevisionNumber(), savedItems);
        });
    }

    @Override
    public OrderResponse confirmOrder(Long orderId, String idempotencyKey) {
        return executeIdempotent(idempotencyKey, OP_CONFIRM_ORDER, HttpStatus.OK.value(), () -> {
            Long staffId = resolveStaffIdForConfirm();
            Order order = getOrderEntity(orderId);
            ensureNoOtherConfirmedOrderInProgress(order);

            if (order.getStatus() == OrderStatus.CREATED) {
                order.confirm(staffId);
                orderRepository.save(order);
            } else if (!REVISION_ALLOWED_STATUSES.contains(order.getStatus())) {
                throw new DomainException("Cannot confirm revision for order in status: " + order.getStatus());
            }

            OrderRevision latestRevision = getLatestRevision(orderId);
            List<OrderItem> latestItems = orderItemRepository.findAllByRevisionIdOrderByIdAsc(latestRevision.getId());

            List<Long> orderItemIds = latestItems.stream()
                    .map(OrderItem::getId)
                    .toList();
            eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), orderItemIds));

            return OrderResponse.from(order, latestRevision.getRevisionNumber(), latestItems);
        });
    }

    @Override
    public OrderResponse cancelOrder(Long orderId, String reason) {
        Order order = getOrderEntity(orderId);
        order.cancel();
        orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCancelledEvent(order.getId(), normalizeCancelReason(reason)));
        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        return toOrderResponse(getOrderEntity(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findAllByStatusOrderByCreatedAtDesc(status);

        return orders.stream()
                .map(this::toOrderResponse)
                .toList();
    }

    private Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private OrderRevision getLatestRevision(Long orderId) {
        return orderRevisionRepository.findTopByOrderIdOrderByRevisionNumberDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderRevision", "orderId=" + orderId));
    }

    private TableDTO validateTableCanReceiveOrders(String tableCode) {
        TableDTO table = catalogService.getTableByCode(tableCode);

        String statusName = table.status() == null ? "UNKNOWN" : table.status().name();
        if (!"AVAILABLE".equals(statusName) && !"OCCUPIED".equals(statusName)) {
            throw new DomainException("Table cannot receive orders in status: " + statusName);
        }

        return table;
    }

    private OrderResponse createRevisionForExistingOrder(
            Order order,
            CreateOrderRequest request,
            RevisionActor actor
    ) {
        ensureCanAddRevision(order);

        Map<Long, MenuItemDTO> menuItems = resolveAvailableMenuItems(request.items());
        int nextRevisionNumber = orderRevisionRepository.findTopByOrderIdOrderByRevisionNumberDesc(order.getId())
                .map(revision -> revision.getRevisionNumber() + 1)
                .orElse(1);

        OrderRevision revision = OrderRevision.create(
            order.getId(),
            nextRevisionNumber,
            actor.source(),
            actor.staffId(),
            actor.qrSessionId()
        );
        revision = orderRevisionRepository.save(revision);

        List<OrderItem> savedItems = orderItemRepository.saveAll(
                buildOrderItemsForCreate(revision.getId(), request.items(), menuItems)
        );

        if (request.note() != null && !request.note().isBlank()) {
            order.updateNote(request.note().trim());
        }

        if (order.getStatus() == OrderStatus.SERVED) {
            order.reopenForAdditionalItems();
            orderRepository.save(order);

            List<Long> orderItemIds = savedItems.stream()
                    .map(OrderItem::getId)
                    .toList();
            eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), orderItemIds));
        }

        refreshOrderTotal(order, revision.getId());
        return OrderResponse.from(order, revision.getRevisionNumber(), savedItems);
    }

    private Map<Long, MenuItemDTO> resolveAvailableMenuItems(List<CreateOrderRequest.OrderItemRequest> items) {
        Map<Long, MenuItemDTO> menuItemsById = new HashMap<>();
        for (CreateOrderRequest.OrderItemRequest item : items) {
            MenuItemDTO menuItem = menuItemsById.computeIfAbsent(item.menuItemId(), catalogService::getItem);
            if (!menuItem.available()) {
                throw new DomainException("Menu item not available: " + item.menuItemId());
            }
        }
        return menuItemsById;
    }

    private Map<Long, MenuItemDTO> resolveAvailableMenuItemsForRevision(
            List<AddRevisionRequest.RevisionItemRequest> items
    ) {
        Map<Long, MenuItemDTO> menuItemsById = new HashMap<>();
        for (AddRevisionRequest.RevisionItemRequest item : items) {
            MenuItemDTO menuItem = menuItemsById.computeIfAbsent(item.menuItemId(), catalogService::getItem);
            if (!menuItem.available()) {
                throw new DomainException("Menu item not available: " + item.menuItemId());
            }
        }
        return menuItemsById;
    }

    private List<OrderItem> buildOrderItemsForCreate(
            Long revisionId,
            List<CreateOrderRequest.OrderItemRequest> requests,
            Map<Long, MenuItemDTO> menuItems
    ) {
        return requests.stream()
                .map(request -> {
                    MenuItemDTO menuItem = menuItems.get(request.menuItemId());
                    return OrderItem.create(
                            revisionId,
                            request.menuItemId(),
                            request.quantity(),
                            menuItem.price(),
                            normalizeOptionalText(request.note())
                    );
                })
                .toList();
    }

    private List<OrderItem> buildOrderItemsForRevision(
            Long revisionId,
            List<AddRevisionRequest.RevisionItemRequest> requests,
            Map<Long, MenuItemDTO> menuItems
    ) {
        return requests.stream()
                .map(request -> {
                    MenuItemDTO menuItem = menuItems.get(request.menuItemId());
                    return OrderItem.create(
                            revisionId,
                            request.menuItemId(),
                            request.quantity(),
                            menuItem.price(),
                            normalizeOptionalText(request.note())
                    );
                })
                .toList();
    }

    private void refreshOrderTotal(Order order, Long revisionId) {
        BigDecimal total = orderItemRepository.sumSubtotalByRevisionId(revisionId);
        order.updateTotalAmount(total);
        orderRepository.save(order);
    }

    private void ensureCanAddRevision(Order order) {
        if (!REVISION_ALLOWED_STATUSES.contains(order.getStatus())) {
            throw new DomainException("Cannot add revision for order in status: " + order.getStatus());
        }
    }

    private void ensureNoOtherConfirmedOrderInProgress(Order order) {
        if (orderRepository.existsByTableIdAndStatusInAndIdNot(
                order.getTableId(),
                ACTIVE_ORDER_STATUSES,
                order.getId()
        )) {
            throw new DomainException("Another order is already being processed for this table: " + order.getTableId());
        }
    }

    private RevisionActor resolveActorForCreate(String qrSessionId, String requestTableCode) {
        if (hasText(qrSessionId)) {
            try {
                catalogService.validateQrSession(qrSessionId, requestTableCode);
            } catch (DomainException ex) {
                throw new InsufficientAuthenticationException(ex.getMessage());
            }
            return new RevisionActor(RevisionSource.CUSTOMER_QR, null, qrSessionId.trim());
        }

        return resolveStaffActor();
    }

    private RevisionActor resolveActorForRevision(String qrSessionId, Order order) {
        if (hasText(qrSessionId)) {
            TableDTO table = catalogService.getTableById(order.getTableId());
            try {
                catalogService.validateQrSession(qrSessionId, table.tableCode());
            } catch (DomainException ex) {
                throw new InsufficientAuthenticationException(ex.getMessage());
            }
            return new RevisionActor(RevisionSource.CUSTOMER_QR, null, qrSessionId.trim());
        }

        return resolveStaffActor();
    }

    private RevisionActor resolveStaffActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new InsufficientAuthenticationException("Either X-QR-Session or JWT Authorization is required");
        }

        boolean hasStaffRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> STAFF_ORDER_ROLES.contains(authority.getAuthority()));
        if (!hasStaffRole) {
            throw new AccessDeniedException("Only staff roles WAITER or MANAGER can create order revisions");
        }

        if (!(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new InsufficientAuthenticationException("Invalid authenticated principal");
        }

        if (principal.getStaffId() == null) {
            throw new InsufficientAuthenticationException("Missing staff id in JWT claims");
        }

        return new RevisionActor(RevisionSource.STAFF, principal.getStaffId(), null);
    }

    private Long resolveStaffIdForConfirm() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new InsufficientAuthenticationException("JWT Authorization is required to confirm order");
        }

        boolean hasConfirmRole = authentication.getAuthorities().stream()
                .anyMatch(authority -> STAFF_CONFIRM_ROLES.contains(authority.getAuthority()));
        if (!hasConfirmRole) {
            throw new AccessDeniedException("Only staff roles WAITER, CASHIER or MANAGER can confirm orders");
        }

        if (!(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new InsufficientAuthenticationException("Invalid authenticated principal");
        }

        if (principal.getStaffId() == null) {
            throw new InsufficientAuthenticationException("Missing staff id in JWT claims");
        }

        return principal.getStaffId();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private OrderResponse executeIdempotent(
            String rawKey,
            String operation,
            int responseStatus,
            Supplier<OrderResponse> action
    ) {
        String normalizedKey = IdempotencyUtil.normalizeKey(rawKey);

        Optional<IdempotencyKey> existingKeyOpt = idempotencyKeyRepository.findByModuleAndOperationAndIdemKey(
            MODULE,
            operation,
            normalizedKey
        );
        if (existingKeyOpt.isPresent()) {
            IdempotencyKey existing = existingKeyOpt.get();
            if (existing.isExpired(Instant.now())) {
                idempotencyKeyRepository.delete(existing);
                idempotencyKeyRepository.flush();
            } else if (existing.hasResponseBody()) {
                return IdempotencyUtil.fromJsonMap(objectMapper, existing.getResponseBody(), OrderResponse.class);
            } else {
                throw new IdempotencyConflictException(normalizedKey);
            }
        }

        IdempotencyKey pendingKey = IdempotencyKey.reserve(
                MODULE,
                operation,
                normalizedKey,
                IdempotencyUtil.defaultExpiry()
        );
        idempotencyKeyRepository.save(pendingKey);

        OrderResponse response = action.get();

        pendingKey.markCompleted(responseStatus, IdempotencyUtil.toJsonMap(objectMapper, response));
        idempotencyKeyRepository.save(pendingKey);

        return response;
    }

    private OrderResponse toOrderResponse(Order order) {
        Optional<OrderRevision> latestRevisionOpt = orderRevisionRepository.findTopByOrderIdOrderByRevisionNumberDesc(order.getId());
        if (latestRevisionOpt.isEmpty()) {
            return OrderResponse.from(order, null, List.of());
        }

        OrderRevision latestRevision = latestRevisionOpt.get();
        List<OrderItem> items = orderItemRepository.findAllByRevisionIdOrderByIdAsc(latestRevision.getId());
        return OrderResponse.from(order, latestRevision.getRevisionNumber(), items);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeCancelReason(String reason) {
        String normalized = normalizeOptionalText(reason);
        return normalized == null ? "Order cancelled" : normalized;
    }

    private record RevisionActor(RevisionSource source, Long staffId, String qrSessionId) {
    }
}
