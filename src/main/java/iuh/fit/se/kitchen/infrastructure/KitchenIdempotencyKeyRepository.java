package iuh.fit.se.kitchen.infrastructure;

import iuh.fit.se.kitchen.domain.KitchenIdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenIdempotencyKeyRepository extends JpaRepository<KitchenIdempotencyKey, Long> {

    Optional<KitchenIdempotencyKey> findByModuleAndOperationAndIdemKey(String module, String operation, String idemKey);
}