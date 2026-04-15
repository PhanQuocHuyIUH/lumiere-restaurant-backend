package iuh.fit.se.billing.infrastructure;

import iuh.fit.se.billing.domain.PaymentTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findAllByPaymentIdOrderByCreatedAtDesc(Long paymentId);
}
