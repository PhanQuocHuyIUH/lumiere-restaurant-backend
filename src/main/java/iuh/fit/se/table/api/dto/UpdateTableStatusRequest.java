package iuh.fit.se.table.api.dto;

import iuh.fit.se.table.domain.TableStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTableStatusRequest(
        @NotNull TableStatus status
) {
}
