package iuh.fit.se.ordering.infrastructure;

import iuh.fit.se.ordering.domain.IdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByModuleAndOperationAndIdemKey(String module, String operation, String idemKey);
}
