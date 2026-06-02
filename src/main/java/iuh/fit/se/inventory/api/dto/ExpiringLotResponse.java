package iuh.fit.se.inventory.api.dto;

import iuh.fit.se.inventory.domain.StockLot;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public record ExpiringLotResponse(
        Long lotId,
        Long ingredientId,
        String ingredientName,
        BigDecimal remainingQty,
        LocalDate expiryDate,
        Instant importedAt,
        long daysUntilExpiry,
        boolean expired
) {

    public static ExpiringLotResponse from(StockLot lot, String ingredientName) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        long days = ChronoUnit.DAYS.between(today, lot.getExpiryDate());
        boolean expired = lot.getExpiryDate().isBefore(today);
        return new ExpiringLotResponse(
                lot.getId(),
                lot.getIngredientId(),
                ingredientName,
                lot.getRemainingQty(),
                lot.getExpiryDate(),
                lot.getImportedAt(),
                days,
                expired
        );
    }
}
