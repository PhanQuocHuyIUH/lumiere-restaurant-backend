package iuh.fit.se.kitchen.infrastructure;

import iuh.fit.se.kitchen.domain.KitchenBatchItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenBatchItemRepository extends JpaRepository<KitchenBatchItem, Long> {

    List<KitchenBatchItem> findAllByBatchId(Long batchId);

    boolean existsByKitchenTaskId(Long kitchenTaskId);
}
