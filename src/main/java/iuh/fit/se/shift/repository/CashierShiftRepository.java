package iuh.fit.se.shift.repository;

import iuh.fit.se.shift.domain.CashierShift;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashierShiftRepository extends JpaRepository<CashierShift, Long> {
    Optional<CashierShift> findTopByClosedAtIsNullOrderByOpenedAtDesc();

    boolean existsByClosedAtIsNull();

    Optional<CashierShift> findByIdAndClosedAtIsNull(Long id);
}
