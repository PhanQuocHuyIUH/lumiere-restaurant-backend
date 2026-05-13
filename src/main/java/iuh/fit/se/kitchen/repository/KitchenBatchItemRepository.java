package iuh.fit.se.kitchen.repository;

import iuh.fit.se.kitchen.domain.KitchenBatchItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenBatchItemRepository extends JpaRepository<KitchenBatchItem, Long> {

    List<KitchenBatchItem> findAllByBatchId(Long batchId);

    List<KitchenBatchItem> findAllByKitchenTaskIdIn(Collection<Long> kitchenTaskIds);

    boolean existsByKitchenTaskId(Long kitchenTaskId);

    void deleteByBatchIdIn(Collection<Long> batchIds);
}
