package iuh.fit.se.kitchen.infrastructure;

import iuh.fit.se.kitchen.domain.KitchenBatch;
import iuh.fit.se.kitchen.domain.KitchenBatchStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenBatchRepository extends JpaRepository<KitchenBatch, Long> {

    List<KitchenBatch> findAllByOrderByCreatedAtDesc();

    List<KitchenBatch> findAllByStatusOrderByCreatedAtDesc(KitchenBatchStatus status);
}
