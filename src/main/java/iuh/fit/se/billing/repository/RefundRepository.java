package iuh.fit.se.billing.repository;

import iuh.fit.se.billing.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}
