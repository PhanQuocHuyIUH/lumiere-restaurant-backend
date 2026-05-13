package iuh.fit.se.identity.application.impl;

import iuh.fit.se.identity.api.dto.CreateStaffRequest;
import iuh.fit.se.identity.api.dto.StaffResponse;
import iuh.fit.se.identity.application.StaffService;
import iuh.fit.se.identity.domain.Staff;
import iuh.fit.se.identity.domain.StaffStatus;
import iuh.fit.se.identity.repository.StaffRepository;
import iuh.fit.se.shared.exception.DomainException;
import iuh.fit.se.shared.exception.ForbiddenException;
import iuh.fit.se.shared.exception.ResourceNotFoundException;
import iuh.fit.se.identity.domain.StaffRole;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffServiceImpl(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public StaffResponse createStaff(CreateStaffRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        ensureUsernameUnique(normalizedUsername);

        Staff staff = Staff.builder()
                .name(normalizeName(request.name()))
                .role(request.role())
                .username(normalizedUsername)
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(StaffStatus.ACTIVE)
                .build();

        if (request.status() == StaffStatus.INACTIVE) {
            staff.deactivate();
        }

        Staff savedStaff = staffRepository.save(staff);
        return StaffResponse.from(savedStaff);
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaffById(Long staffId) {
        return StaffResponse.from(getActiveStaff(staffId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> getAllStaff() {
        return staffRepository.findAllByDeletedAtIsNullOrderByIdAsc()
                .stream()
                .map(StaffResponse::from)
                .toList();
    }

    @Override
    public StaffResponse updateStaff(Long staffId, CreateStaffRequest request) {
        if (staffId.equals(1L) && request.role() != StaffRole.MANAGER) {
            throw new ForbiddenException("Không thể hạ quyền tài khoản quản trị viên gốc của hệ thống");
        }
        Staff staff = getActiveStaff(staffId);
        String normalizedUsername = normalizeUsername(request.username());

        if (staffRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNullAndIdNot(normalizedUsername, staffId)) {
            throw new DomainException("Username already exists: " + normalizedUsername);
        }

        staff.updateProfile(normalizeName(request.name()), request.role());
        staff.changeUsername(normalizedUsername);
        staff.changePasswordHash(passwordEncoder.encode(request.password()));
        applyStatus(staff, request.status());

        Staff updatedStaff = staffRepository.save(staff);
        return StaffResponse.from(updatedStaff);
    }

    @Override
    public void deleteStaff(Long staffId) {
        if (staffId.equals(1L)) {
            throw new ForbiddenException("Không thể xóa tài khoản quản trị viên gốc của hệ thống");
        }
        Staff staff = getActiveStaff(staffId);
        staff.softDelete();
        staffRepository.save(staff);
    }

    private void ensureUsernameUnique(String username) {
        if (staffRepository.existsByUsernameIgnoreCaseAndDeletedAtIsNull(username)) {
            throw new DomainException("Username already exists: " + username);
        }
    }

    private Staff getActiveStaff(Long staffId) {
        return staffRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
    }

    private void applyStatus(Staff staff, StaffStatus requestedStatus) {
        if (requestedStatus == null) {
            return;
        }
        if (requestedStatus == StaffStatus.ACTIVE) {
            staff.activate();
            return;
        }
        staff.deactivate();
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        String normalized = name.trim();
        if (normalized.isEmpty()) {
            throw new DomainException("Staff name must not be blank");
        }
        return normalized;
    }
}
