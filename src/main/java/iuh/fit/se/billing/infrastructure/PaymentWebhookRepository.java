package iuh.fit.se.billing.infrastructure;

import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.PaymentWebhook;
import iuh.fit.se.billing.domain.WebhookStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {

    List<PaymentWebhook> findAllByProviderAndStatusOrderByReceivedAtDesc(PaymentProvider provider, WebhookStatus status);
}
