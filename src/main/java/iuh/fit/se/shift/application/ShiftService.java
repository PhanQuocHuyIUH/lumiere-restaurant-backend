package iuh.fit.se.shift.application;

import iuh.fit.se.shift.api.dto.CloseShiftRequest;
import iuh.fit.se.shift.api.dto.CloseShiftResponse;
import iuh.fit.se.shift.api.dto.OpenShiftRequest;
import iuh.fit.se.shift.api.dto.ShiftResponse;
import java.util.List;

public interface ShiftService {
    ShiftResponse openShift(OpenShiftRequest request);
    CloseShiftResponse closeShift(Long shiftId, CloseShiftRequest request);
    ShiftResponse getCurrentShift();
    List<ShiftResponse> listAll();
    boolean isShiftOpen(Long shiftId);
}
