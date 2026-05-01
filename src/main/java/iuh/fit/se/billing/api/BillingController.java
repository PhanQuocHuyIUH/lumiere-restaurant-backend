package iuh.fit.se.billing.api;

import iuh.fit.se.billing.api.dto.CreatePaymentRequest;
import iuh.fit.se.billing.api.dto.PaymentResponse;
import iuh.fit.se.billing.api.dto.RefundRequest;
import iuh.fit.se.billing.application.BillingService;
import iuh.fit.se.billing.api.dto.InvoiceResponse;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = billingService.createPayment(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment created", response));
    }

    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatusByOrderId(@PathVariable("orderId") Long orderId) {
        PaymentResponse response = billingService.getPaymentStatusByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceForOrder(@PathVariable("orderId") Long orderId) {
        InvoiceResponse response = billingService.getInvoiceForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> createRefund(
            @PathVariable("id") Long paymentId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RefundRequest request
    ) {
        PaymentResponse response = billingService.createRefund(paymentId, request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.ok("Refund processed", response));
    }
}
