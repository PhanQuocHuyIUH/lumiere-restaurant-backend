package iuh.fit.se.billing.api;

import iuh.fit.se.billing.api.dto.PaymentRequestResponse;
import iuh.fit.se.billing.application.PaymentRequestService;
import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.shared.security.JwtPrincipal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payment-requests")
@PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
public class PaymentRequestController {

    private final PaymentRequestService service;

    public PaymentRequestController(PaymentRequestService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentRequestResponse>>> list(
            @RequestParam(value = "status", required = false) List<String> statuses
    ) {
        return ResponseEntity.ok(ApiResponse.ok(service.listByStatuses(statuses)));
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<PaymentRequestResponse>> acknowledge(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        Long staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponse.ok("Payment request acknowledged", service.acknowledge(id, staffId)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentRequestResponse>> cancel(
            @PathVariable("id") Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body == null ? null : body.get("reason");
        return ResponseEntity.ok(ApiResponse.ok("Payment request cancelled", service.cancel(id, reason)));
    }
}
