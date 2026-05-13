package iuh.fit.se.billing.repository;

import iuh.fit.se.billing.domain.PaymentTransaction;
import iuh.fit.se.billing.domain.TxnStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

        @Query("""
                        select pt
                        from PaymentTransaction pt
                        where (:fromTime is null or pt.createdAt >= :fromTime)
                            and (:toTime is null or pt.createdAt < :toTime)
                            and (:status is null or pt.status = :status)
                        """)
        Page<PaymentTransaction> searchHistoryForAiExport(
                        @Param("fromTime") Instant fromTime,
                        @Param("toTime") Instant toTime,
                        @Param("status") TxnStatus status,
                        Pageable pageable
        );

    List<PaymentTransaction> findAllByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}
