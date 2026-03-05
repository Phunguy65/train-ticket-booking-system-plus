package io.github.phunguy65.ttbs.backend.shared.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public final class Money implements ValueObject {

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount);
        this.currency = Objects.requireNonNull(currency);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money vnd(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("VND"));
    }

    public static Money vnd(long amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("VND"));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    /**
     * Returns the amount as a {@code long} in the lowest denomination.
     * Suitable for persisting to a {@code BIGINT} column.
     *
     * @throws ArithmeticException if the amount has a non-zero fractional part
     */
    public long toLong() {
        return amount.longValueExact();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}
