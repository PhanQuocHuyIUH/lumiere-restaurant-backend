package iuh.fit.se.shift.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ShiftResponse(
        Long id,
        Long cashierId,
        Long openedBy,
        Long closedBy,
        Instant openedAt,
        Instant closedAt,
        BigDecimal openingTotal,
        BigDecimal closingTotal,
        String openingNotes,
        String closingNotes
) {}
