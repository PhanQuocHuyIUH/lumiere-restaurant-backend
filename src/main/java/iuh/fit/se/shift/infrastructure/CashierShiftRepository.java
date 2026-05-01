package iuh.fit.se.shift.infrastructure;

import iuh.fit.se.shift.domain.CashierShift;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashierShiftRepository extends JpaRepository<CashierShift, Long> {
    Optional<CashierShift> findTopByCashierIdAndClosedAtIsNullOrderByOpenedAtDesc(Long cashierId);
}
