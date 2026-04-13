package iuh.fit.se.ordering.infrastructure;

import iuh.fit.se.ordering.domain.OrderRevision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRevisionRepository extends JpaRepository<OrderRevision, Long> {

    Optional<OrderRevision> findTopByOrderIdOrderByRevisionNumberDesc(Long orderId);
}
