package iuh.fit.se.serving.api;

import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.serving.application.ServingService;
import iuh.fit.se.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class ServingController {

    private final ServingService servingService;

    public ServingController(ServingService servingService) {
        this.servingService = servingService;
    }

    @PutMapping("/{id}/items/{itemId}/serve")
    public ResponseEntity<ApiResponse<OrderResponse>> serveItem(
            @PathVariable("id") Long orderId,
            @PathVariable("itemId") Long orderItemId
    ) {
        OrderResponse response = servingService.serveItem(orderId, orderItemId);
        return ResponseEntity.ok(ApiResponse.ok("Order item served", response));
    }

    @PutMapping("/{id}/serve-all")
    public ResponseEntity<ApiResponse<OrderResponse>> serveAllItems(
            @PathVariable("id") Long orderId
    ) {
        OrderResponse response = servingService.serveAllItems(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order served", response));
    }
}
