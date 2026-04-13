package iuh.fit.se.ordering.infrastructure;

import iuh.fit.se.ordering.domain.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByRevisionIdOrderByIdAsc(Long revisionId);

    @Query("""
            select coalesce(sum(oi.subtotal), 0)
            from OrderItem oi
            where oi.revisionId = :revisionId
            """)
    BigDecimal sumSubtotalByRevisionId(Long revisionId);
}
