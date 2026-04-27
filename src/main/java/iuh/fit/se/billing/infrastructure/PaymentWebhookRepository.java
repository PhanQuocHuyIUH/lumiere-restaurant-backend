package iuh.fit.se.billing.infrastructure;

import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.PaymentWebhook;
import iuh.fit.se.billing.domain.WebhookStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentWebhookRepository extends JpaRepository<PaymentWebhook, Long> {

        @Query("""
                        select pw
                        from PaymentWebhook pw
                        where (:fromTime is null or pw.receivedAt >= :fromTime)
                            and (:toTime is null or pw.receivedAt < :toTime)
                            and (:provider is null or pw.provider = :provider)
                            and (:status is null or pw.status = :status)
                        """)
        Page<PaymentWebhook> searchHistoryForAiExport(
                        @Param("fromTime") Instant fromTime,
                        @Param("toTime") Instant toTime,
                        @Param("provider") PaymentProvider provider,
                        @Param("status") WebhookStatus status,
                        Pageable pageable
        );

    List<PaymentWebhook> findAllByProviderAndStatusOrderByReceivedAtDesc(PaymentProvider provider, WebhookStatus status);
}
