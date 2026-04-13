package iuh.fit.se.serving.application;

import iuh.fit.se.ordering.api.dto.OrderResponse;

public interface ServingService {

    void handleKitchenTaskDone(Long orderId);

    OrderResponse serveItem(Long orderId, Long orderItemId, String idempotencyKey);

    OrderResponse serveAllItems(Long orderId, String idempotencyKey);
}
