package iuh.fit.se.ordering.application;

import iuh.fit.se.ordering.api.dto.AddRevisionRequest;
import iuh.fit.se.ordering.api.dto.CreateOrderRequest;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.shared.response.PagedResponse;
import java.util.List;
import java.util.Optional;

public interface OrderingService {

    OrderResponse createOrder(CreateOrderRequest request, String qrSessionId);

    OrderResponse addRevision(Long orderId, AddRevisionRequest request, String qrSessionId);

    OrderResponse confirmOrder(Long orderId);

    OrderResponse cancelOrder(Long orderId, String reason);

    OrderResponse cancelOrderItem(Long orderId, Long itemId);

    OrderResponse cancelOrderItemByKitchen(Long orderId, Long itemId);

    OrderResponse markOrderPaid(Long orderId);

    OrderResponse getOrderDetail(Long orderId);

    List<OrderResponse> getOrders(OrderStatus status);

    PagedResponse<OrderResponse> getOrdersPaged(OrderStatus status, int page, int size);

    Long markOrderItemPreparing(Long orderItemId);

    Long markOrderItemDone(Long orderItemId);

    Optional<OrderResponse> markOrderReadyIfAllItemsDone(Long orderId);

    OrderResponse serveOrderItem(Long orderId, Long orderItemId, Long staffId);

    OrderResponse serveAllOrderItems(Long orderId, Long staffId);
}
