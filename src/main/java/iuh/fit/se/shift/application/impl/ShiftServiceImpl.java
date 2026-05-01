package iuh.fit.se.shift.application.impl;

import iuh.fit.se.shift.api.dto.OpenShiftRequest;
import iuh.fit.se.shift.api.dto.ShiftResponse;
import iuh.fit.se.shift.application.ShiftService;
import iuh.fit.se.shift.domain.CashierShift;
import iuh.fit.se.shift.infrastructure.CashierShiftRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShiftServiceImpl implements ShiftService {

    private final CashierShiftRepository repository;

    public ShiftServiceImpl(CashierShiftRepository repository) {
        this.repository = repository;
    }

    @Override
    public ShiftResponse openShift(OpenShiftRequest request) {
        CashierShift shift = new CashierShift(request.cashierId(), request.openingTotal(), request.notes());
        shift = repository.save(shift);
        return map(shift);
    }

    @Override
    public ShiftResponse closeShift(Long shiftId, java.math.BigDecimal closingTotal) {
        CashierShift shift = repository.findById(shiftId).orElseThrow();
        shift.close(closingTotal);
        repository.save(shift);
        return map(shift);
    }

    @Override
    public ShiftResponse getCurrentForCashier(Long cashierId) {
        return repository.findTopByCashierIdAndClosedAtIsNullOrderByOpenedAtDesc(cashierId)
                .map(this::map).orElse(null);
    }

    @Override
    public List<ShiftResponse> listAll() {
        return repository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    private ShiftResponse map(CashierShift s) {
        return new ShiftResponse(s.getId(), s.getCashierId(), s.getOpenedAt(), s.getClosedAt(), s.getOpeningTotal(), s.getClosingTotal(), s.getNotes());
    }
}
