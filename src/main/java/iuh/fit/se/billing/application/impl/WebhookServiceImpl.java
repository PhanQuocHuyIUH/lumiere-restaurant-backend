package iuh.fit.se.billing.application.impl;

import iuh.fit.se.billing.application.WebhookProcessResult;
import iuh.fit.se.billing.application.WebhookService;
import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.VnpayMessageMapper;
import iuh.fit.se.shared.exception.DomainException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookServiceImpl implements WebhookService {
    private static final Set<String> VNPAY_REQUIRED_IPN_FIELDS = Set.of(
            "vnp_TmnCode",
            "vnp_Amount",
            "vnp_OrderInfo",
            "vnp_TransactionNo",
            "vnp_ResponseCode",
            "vnp_TransactionStatus",
            "vnp_TxnRef",
            "vnp_SecureHash"
    );

    @Value("${app.payment.vnpay.hash-secret:}")
    private String vnpayHashSecret;

    @Override
    public boolean verifyVnpaySignature(Map<String, String> payload, String signature) {
        if (!hasText(signature) || !hasText(vnpayHashSecret)) {
            return false;
        }

        String hashType = normalizeHashType(payload.get("vnp_SecureHashType"));
        if (hashType != null && !"SHA512".equals(hashType) && !"HMACSHA512".equals(hashType)) {
            return false;
        }

        String hashData = buildCanonicalString(payload, Set.of("vnp_SecureHash", "vnp_SecureHashType"), true);
        String expected = hmac("HmacSHA512", vnpayHashSecret, hashData);
        return expected.equalsIgnoreCase(signature.trim());
    }

    @Override
    public WebhookProcessResult processIPN(
            PaymentProvider provider,
            String httpMethod,
            Map<String, Object> payload,
            String signature
    ) {
        Map<String, String> normalized = toStringMap(payload);
        String normalizedMethod = normalizeMethod(httpMethod);

        return switch (provider) {
            case VNPAY -> processVnpayWebhook(normalizedMethod, normalized, signature);
            default -> throw new DomainException("Unsupported webhook provider: " + provider);
        };
    }

    private WebhookProcessResult processVnpayWebhook(
            String httpMethod,
            Map<String, String> payload,
            String signature
    ) {
        String signatureValue = hasText(signature) ? signature : payload.get("vnp_SecureHash");
        boolean valid = verifyVnpaySignature(payload, signatureValue);

        boolean hasRequiredFields = hasRequiredVnpayIpnFields(payload);
        String responseCode = hasRequiredFields
                ? VnpayMessageMapper.normalizeResponseCode(payload.get("vnp_ResponseCode"))
                : "99";
        String transactionStatus = hasRequiredFields
                ? VnpayMessageMapper.normalizeTransactionStatus(payload.get("vnp_TransactionStatus"))
                : "99";

        boolean providerSuccess = hasRequiredFields
                && "00".equals(responseCode)
                && "00".equals(transactionStatus);
        boolean success = valid && providerSuccess;

        String message;
        if (!valid) {
            message = VnpayMessageMapper.ipnResponseMessage("97");
        } else if (!hasRequiredFields) {
            message = VnpayMessageMapper.ipnResponseMessage("99");
        } else {
            message = resolveVnpayMessage(responseCode, transactionStatus);
        }

        Long paymentId = parseLong(payload.get("vnp_TxnRef"));
        String providerTransactionId = firstNonBlank(payload.get("vnp_TransactionNo"), payload.get("vnp_BankTranNo"));

        Map<String, Object> normalizedPayload = new LinkedHashMap<>(payload);
        normalizedPayload.put("vnp_ResponseCode", responseCode);
        normalizedPayload.put("vnp_TransactionStatus", transactionStatus);
        normalizedPayload.put("vnp_ResponseMessageVi", VnpayMessageMapper.responseCodeMessage(responseCode));
        normalizedPayload.put("vnp_TransactionStatusMessageVi", VnpayMessageMapper.transactionStatusMessage(transactionStatus));
        normalizedPayload.put("vnp_HttpMethod", httpMethod);

        return new WebhookProcessResult(
                valid,
                success,
                responseCode,
                message,
                paymentId,
                providerTransactionId,
                normalizedPayload
        );
    }

    private boolean hasRequiredVnpayIpnFields(Map<String, String> payload) {
        for (String field : VNPAY_REQUIRED_IPN_FIELDS) {
            if (!hasText(payload.get(field))) {
                return false;
            }
        }
        return true;
    }

    private String resolveVnpayMessage(String responseCode, String transactionStatus) {
        if ("00".equals(responseCode) && !"00".equals(transactionStatus)) {
            return VnpayMessageMapper.transactionStatusMessage(transactionStatus);
        }
        return VnpayMessageMapper.responseCodeMessage(responseCode);
    }

    private Map<String, String> toStringMap(Map<String, Object> payload) {
        Map<String, String> result = new LinkedHashMap<>();
        if (payload == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }

    private Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String buildCanonicalString(Map<String, String> payload, Set<String> excludedKeys, boolean urlEncode) {
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (entry.getKey() == null || excludedKeys.contains(entry.getKey())) {
                continue;
            }
            if (!hasText(entry.getValue())) {
                continue;
            }
            sorted.put(entry.getKey(), entry.getValue().trim());
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }

            if (urlEncode) {
                builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            } else {
                builder.append(entry.getKey()).append('=').append(entry.getValue());
            }
        }

        return builder.toString();
    }

    private String hmac(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHex(bytes);
        } catch (Exception ex) {
            throw new DomainException("Unable to verify webhook signature");
        }
    }

    private String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte b : value) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String firstNonBlank(String first, String second) {
        if (hasText(first)) {
            return first;
        }
        return hasText(second) ? second : null;
    }

    private String normalizeMethod(String method) {
        if (!hasText(method)) {
            return "POST";
        }
        return method.trim().toUpperCase();
    }

    private String normalizeHashType(String hashType) {
        if (!hasText(hashType)) {
            return null;
        }
        return hashType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
