package iuh.fit.se.serving.application;

import iuh.fit.se.ordering.api.dto.OrderResponse;

public interface ServingService {

    void handleKitchenTaskDone(Long orderId);

    OrderResponse serveItem(Long orderId, Long orderItemId);

    OrderResponse serveAllItems(Long orderId);
}
