package io.github.phunguy65.ttbs.backend.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Money")
class MoneyTest {

    private static final Currency VND = Currency.getInstance("VND");

    @Nested
    @DisplayName("vnd(long)")
    class VndLong {

        @Test
        @DisplayName("creates VND money with correct amount")
        void vndLong_createsVndMoneyWithCorrectAmount() {
            Money money = Money.vnd(500_000L);

            assertThat(money.getCurrency()).isEqualTo(VND);
            assertThat(money.toLong()).isEqualTo(500_000L);
        }
    }

    @Nested
    @DisplayName("vnd(BigDecimal)")
    class VndBigDecimal {

        @Test
        @DisplayName("creates VND money with correct amount")
        void vndBigDecimal_createsVndMoneyWithCorrectAmount() {
            Money money = Money.vnd(new BigDecimal("650000"));

            assertThat(money.getCurrency()).isEqualTo(VND);
            assertThat(money.toLong()).isEqualTo(650_000L);
        }
    }

    @Nested
    @DisplayName("of(BigDecimal, Currency)")
    class Of {

        @Test
        @DisplayName("creates money with given currency")
        void of_createsMoney() {
            Currency usd = Currency.getInstance("USD");
            Money money = Money.of(new BigDecimal("100"), usd);

            assertThat(money.getCurrency()).isEqualTo(usd);
            assertThat(money.getAmount()).isEqualByComparingTo(new BigDecimal("100"));
        }
    }

    @Nested
    @DisplayName("toLong()")
    class ToLong {

        @Test
        @DisplayName("returns exact long value for whole amount")
        void toLong_returnsExactValue() {
            assertThat(Money.vnd(999_000L).toLong()).isEqualTo(999_000L);
        }

        @Test
        @DisplayName("throws ArithmeticException for fractional amount")
        void toLong_throwsForFractional() {
            Money money = Money.vnd(new BigDecimal("500.5"));

            assertThatThrownBy(money::toLong).isInstanceOf(ArithmeticException.class);
        }
    }

    @Nested
    @DisplayName("equals()")
    class Equals {

        @Test
        @DisplayName(
                "uses compareTo — new BigDecimal(\"500000\") equals BigDecimal.valueOf(500000)")
        void equals_usesCompareTo_regardlessOfScale() {
            Money a = Money.vnd(new BigDecimal("500000"));
            Money b = Money.vnd(BigDecimal.valueOf(500_000L));

            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("different currencies are not equal")
        void equals_differentCurrencies_notEqual() {
            Money vnd = Money.vnd(500_000L);
            Money usd = Money.of(new BigDecimal("500000"), Currency.getInstance("USD"));

            assertThat(vnd).isNotEqualTo(usd);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("format is \"amount CURRENCY\"")
        void toString_formatsAmountAndCurrency() {
            String result = Money.vnd(500_000L).toString();

            assertThat(result).isEqualTo("500000 VND");
        }
    }
}
