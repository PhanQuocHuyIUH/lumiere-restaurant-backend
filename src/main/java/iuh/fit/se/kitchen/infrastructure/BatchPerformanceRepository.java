package iuh.fit.se.kitchen.infrastructure;

import iuh.fit.se.kitchen.domain.BatchPerformance;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchPerformanceRepository extends JpaRepository<BatchPerformance, Long> {

    Optional<BatchPerformance> findByBatchId(Long batchId);
}
