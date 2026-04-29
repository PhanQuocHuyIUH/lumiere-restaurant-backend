package iuh.fit.se.billing.infrastructure;

import iuh.fit.se.billing.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
