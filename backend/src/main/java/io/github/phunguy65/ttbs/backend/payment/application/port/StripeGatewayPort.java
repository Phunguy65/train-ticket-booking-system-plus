package io.github.phunguy65.ttbs.backend.payment.application.port;

import io.github.phunguy65.ttbs.backend.payment.application.command.CreateCheckoutSessionCommand;

/**
 * Port abstracting all Stripe SDK calls.
 *
 * <p>Implemented by {@code StripeGatewayAdapter} in the infrastructure layer.
 */
public interface StripeGatewayPort {

    record CheckoutSessionResult(String sessionId, String checkoutUrl) {}

    /**
     * Creates a Stripe Checkout Session and returns the session ID and hosted checkout URL.
     *
     * @param command the session parameters
     * @return the session ID and checkout URL
     */
    CheckoutSessionResult createCheckoutSession(CreateCheckoutSessionCommand command);

    /**
     * Expires an active Stripe Checkout Session.
     * Handles the case where the session is already expired gracefully.
     *
     * @param sessionId the Stripe session ID
     */
    void expireCheckoutSession(String sessionId);

    /**
     * Issues a full refund for the given payment intent.
     *
     * @param paymentIntentId the Stripe payment intent ID
     * @param idempotencyKey  deterministic key to prevent duplicate refunds
     */
    void createRefund(String paymentIntentId, String idempotencyKey);
}
