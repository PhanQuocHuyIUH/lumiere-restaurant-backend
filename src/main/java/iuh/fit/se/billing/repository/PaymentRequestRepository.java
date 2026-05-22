package iuh.fit.se.billing.repository;

import iuh.fit.se.billing.domain.PaymentRequest;
import iuh.fit.se.billing.domain.PaymentRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, Long> {

    /**
     * Returns the single active PaymentRequest for an order, if any.
     * Active = status in (REQUESTED, ACKNOWLEDGED). DB has a partial unique index
     * so there is at most one row at a time.
     *
     * Implemented as a Spring Data derived query (rather than JPQL with inline
     * enum literals) so Hibernate binds the values through a parameter and uses
     * the column's `columnDefinition` (payment_request_status_enum) for the PG
     * enum cast. Inline JPQL enum literals were producing
     * `?::PaymentRequestStatus` casts that don't match the DB type name.
     */
    default Optional<PaymentRequest> findActiveByOrderId(Long orderId) {
        return findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
                orderId,
                List.of(PaymentRequestStatus.REQUESTED, PaymentRequestStatus.ACKNOWLEDGED)
        );
    }

    Optional<PaymentRequest> findFirstByOrderIdAndStatusInOrderByCreatedAtDesc(
            Long orderId, List<PaymentRequestStatus> statuses);

    List<PaymentRequest> findAllByStatusInOrderByCreatedAtAsc(List<PaymentRequestStatus> statuses);
}
