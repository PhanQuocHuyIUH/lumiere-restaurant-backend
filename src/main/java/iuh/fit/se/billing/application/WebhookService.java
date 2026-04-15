package iuh.fit.se.billing.application;

import iuh.fit.se.billing.domain.PaymentProvider;
import java.util.Map;

public interface WebhookService {

    boolean verifyVnpaySignature(Map<String, String> payload, String signature);

    WebhookProcessResult processIPN(PaymentProvider provider, String httpMethod, Map<String, Object> payload, String signature);
}
