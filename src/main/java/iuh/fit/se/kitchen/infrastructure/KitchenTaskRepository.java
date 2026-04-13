package iuh.fit.se.kitchen.infrastructure;

import iuh.fit.se.kitchen.domain.KitchenTask;
import iuh.fit.se.kitchen.domain.KitchenTaskStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenTaskRepository extends JpaRepository<KitchenTask, Long> {

    List<KitchenTask> findAllByOrderByIdDesc();

    List<KitchenTask> findAllByStatusOrderByIdDesc(KitchenTaskStatus status);

    List<KitchenTask> findAllByStatusInOrderByIdDesc(Collection<KitchenTaskStatus> statuses);

    List<KitchenTask> findAllByOrderItemIdIn(Collection<Long> orderItemIds);

    Optional<KitchenTask> findByOrderItemId(Long orderItemId);

    boolean existsByOrderItemId(Long orderItemId);
}
