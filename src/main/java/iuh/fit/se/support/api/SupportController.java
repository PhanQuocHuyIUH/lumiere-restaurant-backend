package iuh.fit.se.support.api;

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
    public ResponseEntity<SupportResponse> create(@Valid @RequestBody CreateSupportRequest request) {
        SupportResponse resp = supportService.create(request);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/table/{tableCode}")
    public ResponseEntity<List<SupportResponse>> listByTable(@PathVariable String tableCode) {
        return ResponseEntity.ok(supportService.listByTable(tableCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupportResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(supportService.get(id));
    }

    // Staff endpoints
    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    @GetMapping
    public ResponseEntity<List<SupportResponse>> listAll() {
        return ResponseEntity.ok(supportService.listAll());
    }

    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    @PutMapping("/{id}/assign")
    public ResponseEntity<SupportResponse> assign(@PathVariable Long id, @RequestParam Long staffId) {
        return ResponseEntity.ok(supportService.assign(id, staffId));
    }

    @PreAuthorize("hasAnyRole('WAITER', 'MANAGER')")
    @PutMapping("/{id}/status")
    public ResponseEntity<SupportResponse> updateStatus(@PathVariable Long id, @RequestParam iuh.fit.se.support.domain.SupportRequestStatus status) {
        return ResponseEntity.ok(supportService.updateStatus(id, status));
    }
}
