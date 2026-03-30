package io.github.phunguy65.ttbs.backend.payment.infrastructure.stripe;

import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionExpireParams;
import io.github.phunguy65.ttbs.backend.payment.StripeGatewayException;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateStripeCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class StripeGatewayAdapter implements StripeGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(StripeGatewayAdapter.class);

    // Stripe minimum session expiry is 30 minutes; backend enforces 15-min window separately
    private static final long SESSION_EXPIRY_SECONDS = 1800L;

    @Override
    public CheckoutSessionResult createCheckoutSession(CreateStripeCheckoutSessionCommand command) {
        try {
            long expiresAt = (System.currentTimeMillis() / 1000L) + SESSION_EXPIRY_SECONDS;

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setSuccessUrl(command.successUrl())
                    .setCancelUrl(command.cancelUrl())
                    .setExpiresAt(expiresAt)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(command.currency().toLowerCase())
                                    .setUnitAmountDecimal(new java.math.BigDecimal(
                                            command.amount().toLong()))
                                    .setProductData(
                                            SessionCreateParams.LineItem.PriceData.ProductData
                                                    .builder()
                                                    .setName("Train Ticket")
                                                    .build())
                                    .build())
                            .build())
                    .putMetadata("bookingId", command.bookingId().value().toString())
                    .putMetadata("userId", command.userId().value().toString())
                    .build();

            Session session = Session.create(params);
            return new CheckoutSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new StripeGatewayException(
                    "Failed to create Stripe checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public void expireCheckoutSession(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            session.expire(SessionExpireParams.builder().build());
            log.info("Expired Stripe checkout session {}", sessionId);
        } catch (InvalidRequestException e) {
            log.info(
                    "Stripe session {} already expired or completed: {}",
                    sessionId,
                    e.getMessage());
        } catch (StripeException e) {
            throw new StripeGatewayException(
                    "Failed to expire Stripe session " + sessionId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void createRefund(String paymentIntentId, String idempotencyKey) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .build();

            RequestOptions options =
                    RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();

            Refund.create(params, options);
            log.info("Refund created for paymentIntentId={}", paymentIntentId);
        } catch (StripeException e) {
            throw new StripeGatewayException(
                    "Failed to create Stripe refund for " + paymentIntentId + ": " + e.getMessage(),
                    e);
        }
    }
}
