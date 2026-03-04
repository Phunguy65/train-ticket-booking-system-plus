package io.github.phunguy65.ttbs.backend.payment.infrastructure.stripe;

import java.math.BigDecimal;

/**
 * Converts VND amounts to Stripe's zero-decimal format.
 *
 * <p>VND is a zero-decimal currency in Stripe — amounts are passed as-is (no ×100 multiplication).
 */
public final class StripeAmountConverter {

    private StripeAmountConverter() {}

    /**
     * Converts a VND {@link BigDecimal} amount to the {@code long} value expected by Stripe.
     * No multiplication is applied because VND is zero-decimal.
     *
     * @param vnd the amount in Vietnamese Dong
     * @return the amount as a long (e.g. 300000 VND → 300000L)
     */
    public static long toStripeAmount(BigDecimal vnd) {
        return vnd.longValue();
    }
}
