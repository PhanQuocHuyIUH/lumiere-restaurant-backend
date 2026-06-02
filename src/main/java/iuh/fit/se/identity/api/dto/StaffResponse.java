package iuh.fit.se.identity.api.dto;

import iuh.fit.se.identity.domain.Staff;
import iuh.fit.se.identity.domain.StaffRole;
import iuh.fit.se.identity.domain.StaffStatus;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffResponse {

    private Long id;
    private String employeeCode;
    private String name;
    private String username;
    private StaffRole role;
    private StaffStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static StaffResponse from(Staff staff) {
        return StaffResponse.builder()
                .id(staff.getId())
                .employeeCode(staff.getEmployeeCode())
                .name(staff.getName())
                .username(staff.getUsername())
                .role(staff.getRole())
                .status(staff.getStatus())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}
