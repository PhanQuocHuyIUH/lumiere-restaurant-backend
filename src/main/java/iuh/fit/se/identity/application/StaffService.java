package iuh.fit.se.identity.application;

import iuh.fit.se.identity.api.dto.CreateStaffRequest;
import iuh.fit.se.identity.api.dto.StaffResponse;
import iuh.fit.se.identity.api.dto.UpdateStaffRequest;
import java.util.List;

public interface StaffService {

    StaffResponse createStaff(CreateStaffRequest request);

    StaffResponse getStaffById(Long staffId);

    List<StaffResponse> getAllStaff();

    StaffResponse updateStaff(Long staffId, UpdateStaffRequest request);

    void deleteStaff(Long staffId);

    void resetPassword(Long staffId, String newPassword);
}
