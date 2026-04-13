package iuh.fit.se.ordering.infrastructure;

import iuh.fit.se.ordering.domain.Order;
import iuh.fit.se.ordering.domain.OrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

    Optional<Order> findTopByTableIdAndStatusInOrderByCreatedAtDesc(Long tableId, Collection<OrderStatus> statuses);

    boolean existsByTableIdAndStatusIn(Long tableId, Collection<OrderStatus> statuses);

    boolean existsByTableIdAndStatusInAndIdNot(Long tableId, Collection<OrderStatus> statuses, Long id);
}
