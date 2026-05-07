package iuh.fit.se.ordering.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import iuh.fit.se.menu.api.dto.admin.MenuItemAdminDetailResponse;
import iuh.fit.se.menu.application.MenuItemDTO;
import iuh.fit.se.menu.application.MenuService;
import iuh.fit.se.table.application.TableDTO;
import iuh.fit.se.table.application.TableService;
import iuh.fit.se.menu.domain.ComboKind;
import iuh.fit.se.menu.domain.MenuItemType;
import iuh.fit.se.ordering.api.dto.AddRevisionRequest;
import iuh.fit.se.ordering.api.dto.CreateOrderRequest;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderItem;
import iuh.fit.se.ordering.domain.OrderItemStatus;
import iuh.fit.se.ordering.domain.OrderRevision;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.ordering.domain.RevisionSource;
import iuh.fit.se.ordering.infrastructure.OrderItemRepository;
import iuh.fit.se.ordering.infrastructure.OrderRepository;
import iuh.fit.se.ordering.infrastructure.OrderRevisionRepository;
import iuh.fit.se.shared.ai.AiClient;
import iuh.fit.se.shared.ai.AiOperation;
import iuh.fit.se.shared.ai.client.dto.ChatbotRequest;
import iuh.fit.se.shared.ai.client.dto.ChatbotResponse;
import iuh.fit.se.shared.ai.client.dto.RecommendRequest;
import iuh.fit.se.shared.ai.client.dto.RecommendResponse;
import iuh.fit.se.shared.event.OrderCancelledEvent;
import iuh.fit.se.shared.event.OrderConfirmedEvent;
import iuh.fit.se.shared.event.OrderCreatedEvent;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.security.JwtPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderingServiceImpl implements OrderingService {

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
    private final MenuService menuService;
    private final TableService tableService;
    private final AiClient aiClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderingServiceImpl(
            OrderRepository orderRepository,
            OrderRevisionRepository orderRevisionRepository,
            OrderItemRepository orderItemRepository,
            MenuService menuService,
            TableService tableService,
            AiClient aiClient,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.orderRepository = orderRepository;
        this.orderRevisionRepository = orderRevisionRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuService = menuService;
        this.tableService = tableService;
        this.aiClient = aiClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, String qrSessionId) {
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

            List<OrderItem> savedItems = persistCreateOrderItems(revision.getId(), request.items(), menuItems);

            refreshOrderTotal(order, revision.getId());
            tableService.markTableOccupied(table.id());

            eventPublisher.publishEvent(new OrderCreatedEvent(order.getId(), order.getTableId()));
            OrderResponse response = OrderResponse.from(order, revision.getRevisionNumber(), savedItems);

            if (actor.source() == RevisionSource.CUSTOMER_QR) {
                messagingTemplate.convertAndSend("/topic/waiter/new-order", response);
                messagingTemplate.convertAndSend("/topic/tables/" + order.getTableId(), response);
            }

            return response;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, String qrSessionId, String idempotencyToken) {
        // idempotency token no longer used in ordering module; keep compatibility
        return createOrder(request, qrSessionId);
    }

    @Override
    public OrderResponse addRevision(Long orderId, AddRevisionRequest request, String qrSessionId) {
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

            List<OrderItem> savedItems = persistRevisionItems(revision.getId(), request.items(), menuItems);

            if (request.note() != null && !request.note().isBlank()) {
                order.updateNote(request.note().trim());
            }

            if (order.getStatus() == OrderStatus.SERVED) {
                order.reopenForAdditionalItems();
                orderRepository.save(order);

                List<Long> orderItemIds = savedItems.stream()
                        .filter(item -> !item.isComboParent())
                        .map(OrderItem::getId)
                        .toList();
                eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), orderItemIds));
            }

            refreshOrderTotal(order, revision.getId());
            return OrderResponse.from(order, revision.getRevisionNumber(), savedItems);
    }

    @Override
    public OrderResponse confirmOrder(Long orderId) {
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
                    .filter(item -> !item.isComboParent())
                    .map(OrderItem::getId)
                    .toList();
            eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), orderItemIds));

            return OrderResponse.from(order, latestRevision.getRevisionNumber(), latestItems);
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
    public OrderResponse markOrderPaid(Long orderId) {
        Order order = getOrderEntity(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            return toOrderResponse(order);
        }

        if (order.getStatus() != OrderStatus.SERVED) {
            throw new DomainException("Cannot mark order as paid when status is: " + order.getStatus());
        }

        order.pay();
        orderRepository.save(order);
        tableService.markTableAvailable(order.getTableId());
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

    @Override
    public RecommendResponse recommend(RecommendRequest request) {
        RecommendRequest safeRequest = normalizeRecommendRequest(request);
        return aiClient.post("/ai/recommend", safeRequest, RecommendResponse.class, AiOperation.RECOMMEND)
                .orElseGet(() -> new RecommendResponse(false, "backend-fallback", List.of(), null));
    }

    @Override
    public ChatbotResponse chatbot(ChatbotRequest request) {
        ChatbotRequest safeRequest = normalizeChatbotRequest(request);
        return aiClient.post("/ai/chatbot", safeRequest, ChatbotResponse.class, AiOperation.CHATBOT)
                .orElseGet(() -> new ChatbotResponse(false, "AI service is temporarily unavailable", List.of()));
    }

    @Override
    public Long markOrderItemPreparing(Long orderItemId) {
        OrderItem orderItem = getOrderItemEntity(orderItemId);
        orderItem.startPreparing();
        orderItemRepository.save(orderItem);

        Long orderId = resolveOrderIdFromOrderItem(orderItem);
        Order order = getOrderEntity(orderId);

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.startPreparing();
            orderRepository.save(order);
        } else if (order.getStatus() != OrderStatus.PREPARING) {
            throw new DomainException("Cannot start preparing item when order is in status: " + order.getStatus());
        }

        return orderId;
    }

    @Override
    public Long markOrderItemDone(Long orderItemId) {
        OrderItem orderItem = getOrderItemEntity(orderItemId);
        orderItem.markDone();
        orderItemRepository.save(orderItem);
        return resolveOrderIdFromOrderItem(orderItem);
    }

    @Override
    public Optional<OrderResponse> markOrderReadyIfAllItemsDone(Long orderId) {
        Order order = getOrderEntity(orderId);
        OrderRevision latestRevision = getLatestRevision(orderId);
        List<OrderItem> latestItems = orderItemRepository.findAllByRevisionIdOrderByIdAsc(latestRevision.getId());

        if (latestItems.isEmpty() || !areAllItemsDoneForReady(latestItems)) {
            return Optional.empty();
        }

        if (order.getStatus() == OrderStatus.PREPARING) {
            order.markReady();
            orderRepository.save(order);
            return Optional.of(OrderResponse.from(order, latestRevision.getRevisionNumber(), latestItems));
        }

        return Optional.empty();
    }

    @Override
    public OrderResponse serveOrderItem(Long orderId, Long orderItemId, Long staffId) {
        ensureValidStaffId(staffId);

        Order order = getOrderEntity(orderId);
        ensureOrderCanBeServed(order);

        OrderRevision latestRevision = getLatestRevision(orderId);
        OrderItem orderItem = getOrderItemEntity(orderItemId);
        ensureOrderItemInLatestRevision(orderId, latestRevision, orderItem);

        if (orderItem.getStatus() == OrderItemStatus.DONE) {
            orderItem.markServed();
            orderItemRepository.save(orderItem);
        } else if (orderItem.getStatus() != OrderItemStatus.SERVED) {
            throw new DomainException("Cannot serve item in status: " + orderItem.getStatus());
        }

        List<OrderItem> latestItems = orderItemRepository.findAllByRevisionIdOrderByIdAsc(latestRevision.getId());
        promoteOrderAfterServingIfEligible(order, staffId, latestItems);

        return OrderResponse.from(order, latestRevision.getRevisionNumber(), latestItems);
    }

    @Override
    public OrderResponse serveAllOrderItems(Long orderId, Long staffId) {
        ensureValidStaffId(staffId);

        Order order = getOrderEntity(orderId);
        ensureOrderCanBeServed(order);

        OrderRevision latestRevision = getLatestRevision(orderId);
        List<OrderItem> latestItems = orderItemRepository.findAllByRevisionIdOrderByIdAsc(latestRevision.getId());

        List<OrderItem> changedItems = new ArrayList<>();
        for (OrderItem item : latestItems) {
            if (item.getStatus() == OrderItemStatus.DONE) {
                item.markServed();
                changedItems.add(item);
            } else if (item.getStatus() != OrderItemStatus.SERVED) {
                throw new DomainException(
                        "Cannot serve all items because item " + item.getId() + " is in status: " + item.getStatus()
                );
            }
        }

        if (!changedItems.isEmpty()) {
            orderItemRepository.saveAll(changedItems);
        }

        promoteOrderAfterServingIfEligible(order, staffId, latestItems);
        return OrderResponse.from(order, latestRevision.getRevisionNumber(), latestItems);
    }

    private Order getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private OrderItem getOrderItemEntity(Long orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", orderItemId));
    }

    private OrderRevision getLatestRevision(Long orderId) {
        return orderRevisionRepository.findTopByOrderIdOrderByRevisionNumberDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderRevision", "orderId=" + orderId));
    }

    private Long resolveOrderIdFromOrderItem(OrderItem orderItem) {
        OrderRevision orderRevision = orderRevisionRepository.findById(orderItem.getRevisionId())
                .orElseThrow(() -> new ResourceNotFoundException("OrderRevision", orderItem.getRevisionId()));
        return orderRevision.getOrderId();
    }

    private void ensureValidStaffId(Long staffId) {
        if (staffId == null || staffId <= 0) {
            throw new DomainException("Missing valid staffId for serving operation");
        }
    }

    private void ensureOrderCanBeServed(Order order) {
        if (order.getStatus() != OrderStatus.PREPARING
                && order.getStatus() != OrderStatus.READY
                && order.getStatus() != OrderStatus.SERVED) {
            throw new DomainException("Order is not ready to serve: " + order.getStatus());
        }
    }

    private void ensureOrderItemInLatestRevision(Long orderId, OrderRevision latestRevision, OrderItem orderItem) {
        if (!latestRevision.getId().equals(orderItem.getRevisionId())) {
            throw new DomainException("Order item " + orderItem.getId() + " does not belong to latest revision of order " + orderId);
        }
    }

    private boolean areAllItemsDoneForReady(List<OrderItem> items) {
        return items.stream().allMatch(item ->
                item.getStatus() == OrderItemStatus.DONE || item.getStatus() == OrderItemStatus.SERVED
        );
    }

    private boolean areAllItemsServed(List<OrderItem> items) {
        return items.stream().allMatch(item -> item.getStatus() == OrderItemStatus.SERVED);
    }

    private void promoteOrderAfterServingIfEligible(Order order, Long staffId, List<OrderItem> latestItems) {
        if (order.getStatus() == OrderStatus.PREPARING && areAllItemsDoneForReady(latestItems)) {
            order.markReady();
            orderRepository.save(order);
        }

        if (order.getStatus() == OrderStatus.READY && areAllItemsServed(latestItems)) {
            order.markServed(staffId);
            orderRepository.save(order);
        }
    }

    private TableDTO validateTableCanReceiveOrders(String tableCode) {
        TableDTO table = tableService.getTableByCode(tableCode);

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

        List<OrderItem> savedItems = persistCreateOrderItems(revision.getId(), request.items(), menuItems);

        if (request.note() != null && !request.note().isBlank()) {
            order.updateNote(request.note().trim());
        }

        if (order.getStatus() == OrderStatus.SERVED) {
            order.reopenForAdditionalItems();
            orderRepository.save(order);

            List<Long> orderItemIds = savedItems.stream()
                    .filter(item -> !item.isComboParent())
                    .map(OrderItem::getId)
                    .toList();
            eventPublisher.publishEvent(new OrderConfirmedEvent(order.getId(), orderItemIds));
        }

        refreshOrderTotal(order, revision.getId());
        OrderResponse response = OrderResponse.from(order, revision.getRevisionNumber(), savedItems);

        if (actor.source() == RevisionSource.CUSTOMER_QR) {
            messagingTemplate.convertAndSend("/topic/waiter/new-order", response);
            messagingTemplate.convertAndSend("/topic/tables/" + order.getTableId(), response);
        }

        return response;
    }

    private Map<Long, MenuItemDTO> resolveAvailableMenuItems(List<CreateOrderRequest.OrderItemRequest> items) {
        Map<Long, MenuItemDTO> menuItemsById = new HashMap<>();
        for (CreateOrderRequest.OrderItemRequest item : items) {
            MenuItemDTO menuItem = menuItemsById.computeIfAbsent(item.menuItemId(), menuService::getItem);
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
            MenuItemDTO menuItem = menuItemsById.computeIfAbsent(item.menuItemId(), menuService::getItem);
            if (!menuItem.available()) {
                throw new DomainException("Menu item not available: " + item.menuItemId());
            }
        }
        return menuItemsById;
    }

    private List<OrderItem> persistCreateOrderItems(
            Long revisionId,
            List<CreateOrderRequest.OrderItemRequest> requests,
            Map<Long, MenuItemDTO> menuItems
    ) {
        List<OrderItem> all = new ArrayList<>();
        List<OrderItem> singlesToSave = new ArrayList<>();

        for (CreateOrderRequest.OrderItemRequest request : requests) {
            MenuItemDTO menuItem = menuItems.get(request.menuItemId());
            if (menuItem != null && menuItem.itemType() == MenuItemType.SINGLE) {
                singlesToSave.addAll(createOrderItemsForRequest(
                        revisionId,
                        request.menuItemId(),
                        request.quantity(),
                        request.note(),
                        null,
                        menuItem
                ));
            } else {
                all.addAll(createOrderItemsForRequest(
                        revisionId,
                        request.menuItemId(),
                        request.quantity(),
                        request.note(),
                        request.comboSelection(),
                        menuItem
                ));
            }
        }

        if (!singlesToSave.isEmpty()) {
            all.addAll(orderItemRepository.saveAll(singlesToSave));
        }
        return all;
    }

    private List<OrderItem> persistRevisionItems(
            Long revisionId,
            List<AddRevisionRequest.RevisionItemRequest> requests,
            Map<Long, MenuItemDTO> menuItems
    ) {
        List<OrderItem> all = new ArrayList<>();
        List<OrderItem> singlesToSave = new ArrayList<>();

        for (AddRevisionRequest.RevisionItemRequest request : requests) {
            MenuItemDTO menuItem = menuItems.get(request.menuItemId());
            if (menuItem != null && menuItem.itemType() == MenuItemType.SINGLE) {
                singlesToSave.addAll(createOrderItemsForRequest(
                        revisionId,
                        request.menuItemId(),
                        request.quantity(),
                        request.note(),
                        null,
                        menuItem
                ));
            } else {
                all.addAll(createOrderItemsForRequest(
                        revisionId,
                        request.menuItemId(),
                        request.quantity(),
                        request.note(),
                        request.comboSelection(),
                        menuItem
                ));
            }
        }

        if (!singlesToSave.isEmpty()) {
            all.addAll(orderItemRepository.saveAll(singlesToSave));
        }
        return all;
    }

    private List<OrderItem> createOrderItemsForRequest(
            Long revisionId,
            Long menuItemId,
            Integer quantity,
            String note,
            CreateOrderRequest.ComboSelection comboSelection,
            MenuItemDTO menuItem
    ) {
        if (menuItem == null) {
            throw new DomainException("Menu item not found: " + menuItemId);
        }

        if (menuItem.itemType() == null || menuItem.itemType() == MenuItemType.SINGLE) {
            return List.of(OrderItem.create(
                    revisionId,
                    menuItemId,
                    quantity,
                    menuItem.price(),
                    normalizeOptionalText(note)
            ));
        }

        if (menuItem.comboKind() == null) {
            throw new DomainException("Combo kind is required for combo item: " + menuItemId);
        }

        if (menuItem.comboKind() == ComboKind.FIXED) {
            return createFixedComboOrderItems(revisionId, menuItemId, quantity, note);
        }

        return createPickComboOrderItems(revisionId, menuItemId, quantity, note, comboSelection);
    }

    private List<OrderItem> createFixedComboOrderItems(
            Long revisionId,
            Long comboItemId,
            Integer comboQuantity,
            String note
    ) {
        var detail = menuService.getMenuItemAdminDetail(comboItemId);
        if (detail.getFixedCombo() == null || detail.getFixedCombo().getComponents() == null || detail.getFixedCombo().getComponents().isEmpty()) {
            throw new DomainException("Fixed combo is missing components: " + comboItemId);
        }

        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("kind", "FIXED");
        snapshot.put("comboItemId", comboItemId);
        snapshot.set("components", objectMapper.valueToTree(detail.getFixedCombo().getComponents()));

        OrderItem parent = OrderItem.createComboParent(
                revisionId,
                comboItemId,
                comboQuantity,
                detail.getPrice(),
                snapshot,
                normalizeOptionalText(note)
        );
        parent = orderItemRepository.save(parent);

        List<OrderItem> childrenToSave = new ArrayList<>();
        for (var component : detail.getFixedCombo().getComponents()) {
            MenuItemDTO componentItem = menuService.getItem(component.getMenuItemId());
            if (!componentItem.available()) {
                throw new DomainException("Menu item not available: " + component.getMenuItemId());
            }
            int childQty = component.getQuantity() * comboQuantity;
            childrenToSave.add(OrderItem.createComboChild(revisionId, parent.getId(), component.getMenuItemId(), childQty, null));
        }
        List<OrderItem> savedChildren = childrenToSave.isEmpty() ? List.of() : orderItemRepository.saveAll(childrenToSave);

        List<OrderItem> result = new ArrayList<>();
        result.add(parent);
        result.addAll(savedChildren);
        return result;
    }

    private List<OrderItem> createPickComboOrderItems(
            Long revisionId,
            Long comboItemId,
            Integer comboQuantity,
            String note,
            CreateOrderRequest.ComboSelection comboSelection
    ) {
        if (comboSelection == null) {
            throw new DomainException("comboSelection is required for pick combo: " + comboItemId);
        }

        var detail = menuService.getMenuItemAdminDetail(comboItemId);
        if (detail.getPickCombo() == null || detail.getPickCombo().getSlots() == null || detail.getPickCombo().getSlots().isEmpty()) {
            throw new DomainException("Pick combo is missing slots: " + comboItemId);
        }

        // validate slots exist and selections satisfy min/max + allowed items
        Map<Long, MenuItemAdminDetailResponse.PickSlot> slotsById = new HashMap<>();
        for (var slot : detail.getPickCombo().getSlots()) {
            slotsById.put(slot.getId(), slot);
        }

        // ensure no duplicate slot selections
        Set<Long> seenSlotIds = new LinkedHashSet<>();
        for (var selectedSlot : comboSelection.slots()) {
            if (!seenSlotIds.add(selectedSlot.slotId())) {
                throw new DomainException("Duplicate slot selection: " + selectedSlot.slotId());
            }

            MenuItemAdminDetailResponse.PickSlot slot = slotsById.get(selectedSlot.slotId());
            if (slot == null) {
                throw new DomainException("Invalid slotId for combo: " + selectedSlot.slotId());
            }

            int selectedCount = selectedSlot.items().stream()
                    .mapToInt(i -> i.quantity() == null ? 0 : i.quantity())
                    .sum();
            if (selectedCount < slot.getMinSelect() || selectedCount > slot.getMaxSelect()) {
                throw new DomainException("Slot selection count out of range for slotId=" + slot.getId());
            }

            Set<Long> allowed = new LinkedHashSet<>(slot.getAllowedItemIds());
            Set<Long> dupCheck = new LinkedHashSet<>();
            for (var item : selectedSlot.items()) {
                if (!dupCheck.add(item.menuItemId())) {
                    throw new DomainException("Duplicate selected item in slotId=" + slot.getId() + ": " + item.menuItemId());
                }
                if (!allowed.contains(item.menuItemId())) {
                    throw new DomainException("Selected item not allowed in slotId=" + slot.getId() + ": " + item.menuItemId());
                }
                MenuItemDTO selectedMenuItem = menuService.getItem(item.menuItemId());
                if (!selectedMenuItem.available()) {
                    throw new DomainException("Menu item not available: " + item.menuItemId());
                }
            }
        }

        // ensure all required slots are present
        for (var slot : detail.getPickCombo().getSlots()) {
            boolean present = comboSelection.slots().stream().anyMatch(s -> s.slotId().equals(slot.getId()));
            if (!present && slot.getMinSelect() > 0) {
                throw new DomainException("Missing required slot selection: " + slot.getId());
            }
        }

        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("kind", "PICK");
        snapshot.put("comboItemId", comboItemId);
        snapshot.set("selection", objectMapper.valueToTree(comboSelection));

        OrderItem parent = OrderItem.createComboParent(
                revisionId,
                comboItemId,
                comboQuantity,
                detail.getPrice(),
                snapshot,
                normalizeOptionalText(note)
        );
        parent = orderItemRepository.save(parent);

        List<OrderItem> childrenToSave = new ArrayList<>();
        for (var slotSel : comboSelection.slots()) {
            for (var selectedItem : slotSel.items()) {
                int childQty = selectedItem.quantity() * comboQuantity;
                childrenToSave.add(OrderItem.createComboChild(revisionId, parent.getId(), selectedItem.menuItemId(), childQty, null));
            }
        }
        List<OrderItem> savedChildren = childrenToSave.isEmpty() ? List.of() : orderItemRepository.saveAll(childrenToSave);

        List<OrderItem> result = new ArrayList<>();
        result.add(parent);
        result.addAll(savedChildren);
        return result;
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
                tableService.validateQrSession(qrSessionId, requestTableCode);
            } catch (DomainException ex) {
                throw new InsufficientAuthenticationException(ex.getMessage());
            }
            return new RevisionActor(RevisionSource.CUSTOMER_QR, null, qrSessionId.trim());
        }

        return resolveStaffActor();
    }

    private RevisionActor resolveActorForRevision(String qrSessionId, Order order) {
        if (hasText(qrSessionId)) {
            TableDTO table = tableService.getTableById(order.getTableId());
            try {
                tableService.validateQrSession(qrSessionId, table.tableCode());
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

    private OrderResponse toOrderResponse(Order order) {
        Optional<OrderRevision> latestRevisionOpt = orderRevisionRepository.findTopByOrderIdOrderByRevisionNumberDesc(order.getId());
        if (latestRevisionOpt.isEmpty()) {
            return OrderResponse.from(order, null, List.of());
        }

        OrderRevision latestRevision = latestRevisionOpt.get();
        List<OrderItem> items = orderItemRepository.findAllByRevisionIdOrderByIdAsc(latestRevision.getId());
        return OrderResponse.from(order, latestRevision.getRevisionNumber(), items);
    }

    private RecommendRequest normalizeRecommendRequest(RecommendRequest request) {
        if (request == null) {
            throw new DomainException("Recommend request is required");
        }

        List<Long> currentItems = request.currentItems() == null
                ? List.of()
                : request.currentItems().stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList();

        if (currentItems.isEmpty()) {
            throw new DomainException("currentItems must not be empty");
        }

        int topK = request.topK() <= 0 ? 3 : request.topK();
        return new RecommendRequest(currentItems, topK);
    }

    private ChatbotRequest normalizeChatbotRequest(ChatbotRequest request) {
        if (request == null) {
            throw new DomainException("Chatbot request is required");
        }

        String sessionId = normalizeOptionalText(request.sessionId());
        if (sessionId == null) {
            throw new DomainException("sessionId is required");
        }

        String message = normalizeOptionalText(request.message());
        if (message == null) {
            throw new DomainException("message is required");
        }

        List<Long> currentCartItemIds = request.currentCartItemIds() == null
                ? List.of()
                : request.currentCartItemIds().stream()
                        .filter(id -> id != null && id > 0)
                        .distinct()
                        .toList();

        return new ChatbotRequest(sessionId, message, currentCartItemIds);
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
