package iuh.fit.se.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyHelper {

    private MoneyHelper() {}

    public static long toStorageUnit(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public static long toStorageUnit(double amount) {
        return toStorageUnit(BigDecimal.valueOf(amount));
    }

    public static BigDecimal fromStorageUnit(long vnd) {
        return BigDecimal.valueOf(vnd);
    }

    public static void validateMoneyAmount(long vnd) {
        if (vnd < 0L) {
            throw new IllegalArgumentException("Money amount must be non-negative: " + vnd);
        }
    }
}
