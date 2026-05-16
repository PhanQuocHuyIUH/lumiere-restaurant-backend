package iuh.fit.se.shift.api;

import iuh.fit.se.shift.api.dto.CloseShiftRequest;
import iuh.fit.se.shift.api.dto.CloseShiftResponse;
import iuh.fit.se.shift.api.dto.OpenShiftRequest;
import iuh.fit.se.shift.api.dto.ShiftResponse;
import iuh.fit.se.shift.api.dto.ShiftSummaryResponse;
import iuh.fit.se.shift.application.ShiftService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    @PostMapping("/open")
    public ResponseEntity<ShiftResponse> open(@Valid @RequestBody OpenShiftRequest request) {
        return ResponseEntity.ok(shiftService.openShift(request));
    }

    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    @PostMapping("/{id}/close")
    public ResponseEntity<CloseShiftResponse> close(@PathVariable Long id, @Valid @RequestBody CloseShiftRequest request) {
        return ResponseEntity.ok(shiftService.closeShift(id, request));
    }

    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    @GetMapping("/{id}/summary")
    public ResponseEntity<ShiftSummaryResponse> summary(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.previewShift(id));
    }

    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER')")
    @GetMapping("/current")
    public ResponseEntity<ShiftResponse> current() {
        return ResponseEntity.ok(shiftService.getCurrentShift());
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping
    public ResponseEntity<List<ShiftResponse>> list() {
        return ResponseEntity.ok(shiftService.listAll());
    }
}
