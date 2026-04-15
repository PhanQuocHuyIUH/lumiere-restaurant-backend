package iuh.fit.se.billing.infrastructure;

import iuh.fit.se.billing.domain.BillingIdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingIdempotencyKeyRepository extends JpaRepository<BillingIdempotencyKey, Long> {

    Optional<BillingIdempotencyKey> findByModuleAndOperationAndIdemKey(String module, String operation, String idemKey);
}
