package iuh.fit.se.inventory.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nhập kho cho 1 nguyên liệu. Hạn dùng của lô có thể khai báo theo 1 trong 2 cách:
 * <ul>
 *   <li>{@code expiryDate} — chọn trực tiếp ngày hết hạn (phải ở tương lai).</li>
 *   <li>{@code shelfLifeDays} — số ngày sử dụng kể từ hôm nay; expiry = hôm nay + số ngày.</li>
 * </ul>
 * Nếu cả hai cùng có, {@code shelfLifeDays} được ưu tiên.
 */
public record ImportStockRequest(
        @NotNull(message = "ingredientId is required")
        Long ingredientId,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be > 0")
        BigDecimal quantity,

        @Future(message = "expiryDate must be in the future")
        LocalDate expiryDate,

        @Positive(message = "shelfLifeDays must be > 0")
        Integer shelfLifeDays,

        String note
) {

    /** Bắt buộc khai báo hạn dùng theo ít nhất 1 trong 2 cách. */
    @AssertTrue(message = "Phải nhập ngày hết hạn hoặc số ngày sử dụng")
    public boolean isExpirySpecified() {
        return expiryDate != null || shelfLifeDays != null;
    }

    /** Ngày hết hạn hiệu lực của lô: ưu tiên {@code shelfLifeDays} (tính từ {@code today}). */
    public LocalDate resolveExpiryDate(LocalDate today) {
        if (shelfLifeDays != null) {
            return today.plusDays(shelfLifeDays);
        }
        return expiryDate;
    }
}
