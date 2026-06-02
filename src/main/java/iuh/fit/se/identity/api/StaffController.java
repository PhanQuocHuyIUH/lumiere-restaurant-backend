package iuh.fit.se.identity.api;

import iuh.fit.se.identity.api.dto.CreateStaffRequest;
import iuh.fit.se.identity.api.dto.ResetPasswordRequest;
import iuh.fit.se.identity.api.dto.StaffResponse;
import iuh.fit.se.identity.api.dto.UpdateStaffRequest;
import iuh.fit.se.identity.application.StaffService;
import iuh.fit.se.shared.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff")
@PreAuthorize("hasRole('MANAGER')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffResponse>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        StaffResponse createdStaff = staffService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Staff created successfully", createdStaff));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffResponse>>> getAllStaff() {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getAllStaff()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> getStaffById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getStaffById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffResponse>> updateStaff(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateStaffRequest request
    ) {
        StaffResponse updatedStaff = staffService.updateStaff(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Staff updated successfully", updatedStaff));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable("id") Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        staffService.resetPassword(id, request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStaff(@PathVariable("id") Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok(ApiResponse.ok("Staff deleted successfully", null));
    }
}
