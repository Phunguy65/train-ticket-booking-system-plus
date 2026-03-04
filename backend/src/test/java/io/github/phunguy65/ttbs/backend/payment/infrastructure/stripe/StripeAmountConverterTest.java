package io.github.phunguy65.ttbs.backend.payment.infrastructure.stripe;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StripeAmountConverterTest {

    @Test
    void toStripeAmount_vnd300000_shouldReturn300000WithoutMultiplication() {
        long result = StripeAmountConverter.toStripeAmount(new BigDecimal("300000"));
        assertThat(result).isEqualTo(300000L);
    }

    @Test
    void toStripeAmount_shouldNotMultiplyBy100() {
        long result = StripeAmountConverter.toStripeAmount(new BigDecimal("500000"));
        assertThat(result).isEqualTo(500000L);
        assertThat(result).isNotEqualTo(50000000L);
    }

    @Test
    void toStripeAmount_smallAmount_shouldReturnCorrectValue() {
        long result = StripeAmountConverter.toStripeAmount(new BigDecimal("1000"));
        assertThat(result).isEqualTo(1000L);
    }

    @Test
    void toStripeAmount_withDecimalPart_shouldTruncate() {
        long result = StripeAmountConverter.toStripeAmount(new BigDecimal("300000.99"));
        assertThat(result).isEqualTo(300000L);
    }
}
