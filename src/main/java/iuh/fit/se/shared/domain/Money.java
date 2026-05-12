package iuh.fit.se.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class Money {

    @Column(name = "vnd_amount", nullable = false)
    private Long vndAmount;

    protected Money() {
    }

    private Money(Long vndAmount) {
        this.vndAmount = vndAmount;
    }

    public static Money ofVnd(long vnd) {
        return new Money(vnd);
    }

    public static Money of(BigDecimal amountVnd) {
        if (amountVnd == null) {
            return new Money(0L);
        }
        long v = amountVnd.setScale(0, RoundingMode.HALF_UP).longValue();
        return new Money(v);
    }

    public long toLong() {
        return vndAmount == null ? 0L : vndAmount;
    }

    public BigDecimal toBigDecimal() {
        return BigDecimal.valueOf(toLong());
    }

    public double doubleValue() {
        return (double) toLong();
    }

    public BigDecimal setScale(int scale, RoundingMode roundingMode) {
        return toBigDecimal().setScale(scale, roundingMode);
    }

    public Money add(Money other) {
        long a = this.toLong();
        long b = other == null ? 0L : other.toLong();
        return new Money(a + b);
    }

    public Money subtract(Money other) {
        long a = this.toLong();
        long b = other == null ? 0L : other.toLong();
        return new Money(a - b);
    }

    public Money multiply(BigDecimal multiplier) {
        BigDecimal result = BigDecimal.valueOf(this.toLong()).multiply(multiplier == null ? BigDecimal.ZERO : multiplier);
        long v = result.setScale(0, RoundingMode.HALF_UP).longValue();
        return new Money(v);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(vndAmount, money.vndAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vndAmount);
    }

    @Override
    public String toString() {
        return String.valueOf(toLong());
    }
}
