package iuh.fit.se.billing.repository;

import iuh.fit.se.billing.domain.Refund;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findAllByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    @Query("""
            SELECT COALESCE(SUM(r.amount.vndAmount), 0)
            FROM Refund r
            WHERE r.paymentId = :paymentId AND r.status = iuh.fit.se.billing.domain.RefundStatus.SUCCESS
            """)
    BigDecimal sumSuccessAmountByPaymentId(@Param("paymentId") Long paymentId);
}
