package iuh.fit.se.shift.application.impl;

import iuh.fit.se.billing.application.BillingService;
import iuh.fit.se.billing.application.dto.ShiftPaymentSummary;
import iuh.fit.se.shift.api.dto.CloseShiftRequest;
import iuh.fit.se.shift.api.dto.CloseShiftResponse;
import iuh.fit.se.shift.api.dto.OpenShiftRequest;
import iuh.fit.se.shift.api.dto.ShiftResponse;
import iuh.fit.se.shift.application.ShiftService;
import iuh.fit.se.shift.domain.CashierShift;
import iuh.fit.se.shift.infrastructure.CashierShiftRepository;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.shared.security.JwtPrincipal;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShiftServiceImpl implements ShiftService {

    private final CashierShiftRepository repository;
    private final BillingService billingService;

    public ShiftServiceImpl(CashierShiftRepository repository, BillingService billingService) {
        this.repository = repository;
        this.billingService = billingService;
    }

    @Override
    public ShiftResponse openShift(OpenShiftRequest request) {
        JwtPrincipal principal = resolvePrincipal();
        validateOpenShiftRequest(request, principal);
        if (repository.existsByClosedAtIsNull()) {
            throw new DomainException("An active shift already exists in the system");
        }

        CashierShift shift = new CashierShift(
                request.cashierId(),
                request.openingTotal(),
                normalizeOptionalText(request.notes()),
                principal.getStaffId()
        );
        shift = repository.save(shift);
        return map(shift);
    }

    @Override
    public CloseShiftResponse closeShift(Long shiftId, CloseShiftRequest request) {
        JwtPrincipal principal = resolvePrincipal();
        CashierShift shift = repository.findById(shiftId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
        if (shift.getClosedAt() != null) {
            throw new DomainException("Shift is already closed");
        }

        Instant closedAt = Instant.now();
        ShiftPaymentSummary summary = billingService.getShiftPaymentSummary(shiftId);
        BigDecimal expectedCash = shift.getOpeningTotal().add(summary.cashRevenue());
        BigDecimal discrepancy = request.actualCash().subtract(expectedCash);
        String closingNotes = normalizeOptionalText(request.notes());

        if (discrepancy.compareTo(BigDecimal.ZERO) != 0 && (closingNotes == null || closingNotes.isBlank())) {
            throw new DomainException("notes is required when discrepancy is not zero");
        }

        shift.close(request.actualCash(), closedAt, principal.getStaffId(), closingNotes);
        repository.save(shift);

        return new CloseShiftResponse(
                shift.getId(),
                shift.getCashierId(),
                shift.getOpenedBy(),
                shift.getClosedBy(),
                shift.getOpenedAt(),
                shift.getClosedAt(),
                shift.getOpeningTotal(),
                summary.cashRevenue(),
                summary.transferRevenue(),
                expectedCash,
                request.actualCash(),
                discrepancy,
                closingNotes
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftResponse getCurrentShift() {
        return repository.findTopByClosedAtIsNullOrderByOpenedAtDesc()
                .map(this::map).orElse(null);
    }

    @Override
    public List<ShiftResponse> listAll() {
        return repository.findAll().stream().map(this::map).collect(Collectors.toList());
    }

    private ShiftResponse map(CashierShift s) {
        return new ShiftResponse(
                s.getId(),
                s.getCashierId(),
                s.getOpenedBy(),
                s.getClosedBy(),
                s.getOpenedAt(),
                s.getClosedAt(),
                s.getOpeningTotal(),
                s.getClosingTotal(),
                s.getNotes(),
                s.getClosingNotes()
        );
    }

    private JwtPrincipal resolvePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new InsufficientAuthenticationException("JWT Authorization is required");
        }

        if (!(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new InsufficientAuthenticationException("Invalid authenticated principal");
        }

        if (principal.getStaffId() == null || principal.getStaffId() <= 0) {
            throw new DomainException("Missing staffId in JWT claims");
        }

        return principal;
    }

    private void validateOpenShiftRequest(OpenShiftRequest request, JwtPrincipal principal) {
        if (request.openingTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainException("openingTotal must be greater than or equal to zero");
        }

        boolean isManager = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> "ROLE_MANAGER".equals(auth.getAuthority()));
        if (!isManager && !request.cashierId().equals(principal.getStaffId())) {
            throw new DomainException("cashierId must match authenticated staffId");
        }
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
