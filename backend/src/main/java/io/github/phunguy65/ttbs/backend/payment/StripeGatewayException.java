package io.github.phunguy65.ttbs.backend.payment;

/**
 * Thrown when the Stripe API call fails due to a gateway-level error
 * (network issue, API error, rate limit, etc.).
 *
 * <p>Translated by {@code GlobalExceptionHandler} to HTTP 502 Bad Gateway
 * so the client receives a clear "payment service unavailable" message
 * instead of a generic 500.
 */
public class StripeGatewayException extends RuntimeException {

    public StripeGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
