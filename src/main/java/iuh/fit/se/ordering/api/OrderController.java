package iuh.fit.se.ordering.api;

import iuh.fit.se.ordering.api.dto.AddRevisionRequest;
import iuh.fit.se.ordering.api.dto.CreateOrderRequest;
import iuh.fit.se.ordering.api.dto.OrderResponse;
import iuh.fit.se.ordering.application.OrderingService;
import iuh.fit.se.ordering.domain.OrderStatus;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderingService orderingService;

    public OrderController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader(value = "X-QR-Session", required = false) String qrSessionId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse response = orderingService.createOrder(request, qrSessionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderingService.getOrderDetail(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam(value = "status", required = false) OrderStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderingService.getOrders(status)));
    }

    @PostMapping("/{id}/revisions")
    public ResponseEntity<ApiResponse<OrderResponse>> addRevision(
            @PathVariable("id") Long orderId,
            @RequestHeader(value = "X-QR-Session", required = false) String qrSessionId,
            @Valid @RequestBody AddRevisionRequest request
    ) {
        OrderResponse response = orderingService.addRevision(orderId, request, qrSessionId);
        return ResponseEntity.ok(ApiResponse.ok("Order revision added", response));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable("id") Long orderId
    ) {
        OrderResponse response = orderingService.confirmOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok("Order confirmed", response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable("id") Long orderId,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        OrderResponse response = orderingService.cancelOrder(orderId, request.reason());
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", response));
    }

    public record CancelOrderRequest(
            @NotBlank(message = "reason is required")
            String reason
    ) {
    }
}
