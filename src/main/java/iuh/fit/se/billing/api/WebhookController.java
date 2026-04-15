package iuh.fit.se.billing.api;

import iuh.fit.se.billing.application.BillingService;
import iuh.fit.se.billing.application.BillingWebhookResult;
import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.VnpayMessageMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/webhooks")
public class WebhookController {

    private final BillingService billingService;

    public WebhookController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/vnpay")
    public ResponseEntity<Map<String, String>> processVnpayWebhookGet(
            @RequestParam MultiValueMap<String, String> params,
            @RequestHeader(value = "X-VNPAY-Signature", required = false) String signature
    ) {
        Map<String, Object> payload = toSingleValuePayload(params);
        BillingWebhookResult result = billingService.processWebhook(PaymentProvider.VNPAY, "GET", payload, signature);
        return ResponseEntity.ok(toVnpayIpnResponse(result));
    }

    @PostMapping("/vnpay")
    public ResponseEntity<Map<String, String>> processVnpayWebhookPost(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-VNPAY-Signature", required = false) String signature
    ) {
        BillingWebhookResult result = billingService.processWebhook(PaymentProvider.VNPAY, "POST", payload, signature);
        return ResponseEntity.ok(toVnpayIpnResponse(result));
    }

    private Map<String, String> toVnpayIpnResponse(BillingWebhookResult result) {
        Map<String, String> response = new LinkedHashMap<>();
        if (!result.signatureValid()) {
            response.put("RspCode", "97");
            response.put("Message", VnpayMessageMapper.ipnResponseMessage("97"));
        } else if ("01".equals(result.responseCode())) {
            response.put("RspCode", "01");
            response.put("Message", VnpayMessageMapper.ipnResponseMessage("01"));
        } else if ("02".equals(result.responseCode())) {
            response.put("RspCode", "02");
            response.put("Message", VnpayMessageMapper.ipnResponseMessage("02"));
        } else if ("04".equals(result.responseCode())) {
            response.put("RspCode", "04");
            response.put("Message", VnpayMessageMapper.ipnResponseMessage("04"));
        } else {
            // Merchant has already recorded the callback result (success/failure), so VNPay should stop retrying.
            response.put("RspCode", "00");
            response.put("Message", VnpayMessageMapper.ipnResponseMessage("00"));
        }

        return response;
    }

    private Map<String, Object> toSingleValuePayload(MultiValueMap<String, String> params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (params == null) {
            return payload;
        }

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            payload.put(entry.getKey(), entry.getValue().get(0));
        }

        return payload;
    }
}
