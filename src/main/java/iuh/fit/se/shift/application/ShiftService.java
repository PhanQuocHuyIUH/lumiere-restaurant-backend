package iuh.fit.se.shift.application;

import iuh.fit.se.shift.api.dto.OpenShiftRequest;
import iuh.fit.se.shift.api.dto.ShiftResponse;
import java.util.List;

public interface ShiftService {
    ShiftResponse openShift(OpenShiftRequest request);
    ShiftResponse closeShift(Long shiftId, java.math.BigDecimal closingTotal);
    ShiftResponse getCurrentForCashier(Long cashierId);
    List<ShiftResponse> listAll();
}
