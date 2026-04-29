package iuh.fit.se.ordering.application;

import iuh.fit.se.ordering.api.dto.AddRevisionRequest;
import iuh.fit.se.ordering.api.dto.CreateOrderRequest;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.domain.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderingService {

    OrderResponse createOrder(CreateOrderRequest request, String qrSessionId);
    
    // Backwards-compatible overload for tests/clients that still pass an idempotency token
    OrderResponse createOrder(CreateOrderRequest request, String qrSessionId, String idempotencyToken);

    OrderResponse addRevision(Long orderId, AddRevisionRequest request, String qrSessionId);

    OrderResponse confirmOrder(Long orderId);

    OrderResponse cancelOrder(Long orderId, String reason);

    OrderResponse markOrderPaid(Long orderId);

    OrderResponse getOrderDetail(Long orderId);

    List<OrderResponse> getOrders(OrderStatus status);

    Long markOrderItemPreparing(Long orderItemId);

    Long markOrderItemDone(Long orderItemId);

    Optional<OrderResponse> markOrderReadyIfAllItemsDone(Long orderId);

    OrderResponse serveOrderItem(Long orderId, Long orderItemId, Long staffId);

    OrderResponse serveAllOrderItems(Long orderId, Long staffId);
}
