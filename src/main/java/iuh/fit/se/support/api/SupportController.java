package iuh.fit.se.support.api;

import iuh.fit.se.shared.response.ApiResponse;
import iuh.fit.se.support.api.dto.CreateSupportRequest;
import iuh.fit.se.support.api.dto.SupportResponse;
import iuh.fit.se.support.application.SupportService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupportResponse>> create(@Valid @RequestBody CreateSupportRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.create(request)));
    }

    @GetMapping("/table/{tableCode}")
    public ResponseEntity<ApiResponse<List<SupportResponse>>> listByTable(@PathVariable String tableCode) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.listByTable(tableCode)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupportResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.get(id)));
    }

    // Staff endpoints
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SupportResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok(supportService.listAll()));
    }

    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    @PutMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<SupportResponse>> assign(@PathVariable Long id, @RequestParam Long staffId) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.assign(id, staffId)));
    }

    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SupportResponse>> updateStatus(@PathVariable Long id, @RequestParam iuh.fit.se.support.domain.SupportRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(supportService.updateStatus(id, status)));
    }
}
