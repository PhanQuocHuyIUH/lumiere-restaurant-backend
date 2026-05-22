package iuh.fit.se.billing.repository;

import iuh.fit.se.billing.domain.Payment;
import iuh.fit.se.billing.domain.PaymentMethod;
import iuh.fit.se.billing.domain.PaymentProvider;
import iuh.fit.se.billing.domain.PaymentStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("""
        select p
        from Payment p
                where p.createdAt >= :fromTime
                    and p.createdAt < :toTime
                    and (:filterByStatus = false or p.status = :status)
        """)
    Page<Payment> searchForAiExport(
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
                @Param("filterByStatus") boolean filterByStatus,
        @Param("status") PaymentStatus status,
        Pageable pageable
    );

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findTopByOrderIdAndStatusOrderByCreatedAtDesc(Long orderId, PaymentStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findAllByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findTopByProviderAndProviderTransactionIdOrderByCreatedAtDesc(
            PaymentProvider provider,
            String providerTransactionId
    );

        @Query("""
                select coalesce(sum(p.amount.vndAmount), 0)
                from Payment p
                where p.shiftId = :shiftId
                    and p.status = :status
                    and p.paymentMethod = :paymentMethod
                """)
        Long sumAmountByShiftIdAndStatusAndPaymentMethod(
                        @Param("shiftId") Long shiftId,
                        @Param("status") PaymentStatus status,
                        @Param("paymentMethod") PaymentMethod paymentMethod
        );

        @Query("""
                select coalesce(sum(p.amount.vndAmount), 0)
                from Payment p
                where p.shiftId = :shiftId
                    and p.status = :status
                    and p.paymentMethod in :paymentMethods
                """)
            Long sumAmountByShiftIdAndStatusAndPaymentMethodIn(
                        @Param("shiftId") Long shiftId,
                        @Param("status") PaymentStatus status,
                        @Param("paymentMethods") Collection<PaymentMethod> paymentMethods
        );

        long countByShiftIdAndStatus(Long shiftId, PaymentStatus status);
}
