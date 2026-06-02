package iuh.fit.se.identity.api.dto;

import iuh.fit.se.identity.domain.StaffRole;
import iuh.fit.se.identity.domain.StaffStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateStaffRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Username is required")
        String username,

        @NotNull(message = "Role is required")
        StaffRole role,

        StaffStatus status
) {
}
