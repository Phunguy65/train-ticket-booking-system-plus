package io.github.phunguy65.ttbs.backend.payment.infrastructure.stripe;

import com.stripe.Stripe;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionExpireParams;
import io.github.phunguy65.ttbs.backend.payment.application.dto.CheckoutSessionDto;
import io.github.phunguy65.ttbs.backend.payment.application.dto.CreateCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.CheckoutSessionPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.CheckoutSessionStatus;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.config.StripeProperties;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class StripeCheckoutAdapter implements CheckoutSessionPort {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutAdapter.class);
    private static final String CURRENCY_VND = "vnd";
    private static final int SESSION_EXPIRY_MINUTES = 30;

    private final StripeProperties stripeProperties;

    StripeCheckoutAdapter(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = stripeProperties.apiKey();
    }

    @Override
    public CheckoutSessionDto createSession(CreateCheckoutSessionCommand command) {
        long expiresAt =
                Instant.now().plus(SESSION_EXPIRY_MINUTES, ChronoUnit.MINUTES).getEpochSecond();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(CURRENCY_VND)
                                .setUnitAmount(
                                        StripeAmountConverter.toStripeAmount(command.amountVnd()))
                                .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                .setName("Train Ticket")
                                                .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                .setSuccessUrl(stripeProperties.successUrl())
                .setCancelUrl(stripeProperties.cancelUrl())
                .setExpiresAt(expiresAt)
                .putMetadata("booking_id", command.bookingId().toString())
                .setAfterExpiration(SessionCreateParams.AfterExpiration.builder()
                        .setRecovery(SessionCreateParams.AfterExpiration.Recovery.builder()
                                .setEnabled(false)
                                .build())
                        .build())
                .build();

        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey(command.idempotencyKey())
                .build();

        try {
            Session session = Session.create(params, requestOptions);
            return new CheckoutSessionDto(
                    session.getId(),
                    session.getUrl(),
                    Instant.ofEpochSecond(session.getExpiresAt()));
        } catch (StripeException ex) {
            throw new RuntimeException(
                    "Failed to create Stripe Checkout Session: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void expireSession(CheckoutSessionId checkoutSessionId) {
        try {
            Session session = Session.retrieve(checkoutSessionId.value());
            // Only expire if still open — avoid errors on already-expired sessions
            if ("open".equals(session.getStatus())) {
                session.expire(SessionExpireParams.builder().build());
            }
        } catch (InvalidRequestException ex) {
            // Session not found or already expired — log and continue
            log.warn(
                    "Could not expire Stripe session {}: {}",
                    checkoutSessionId.value(),
                    ex.getMessage());
        } catch (StripeException ex) {
            throw new RuntimeException(
                    "Failed to expire Stripe session " + checkoutSessionId.value() + ": "
                            + ex.getMessage(),
                    ex);
        }
    }

    @Override
    public CheckoutSessionStatus retrieveSession(CheckoutSessionId checkoutSessionId) {
        try {
            Session session = Session.retrieve(checkoutSessionId.value());
            return mapStatus(session.getStatus());
        } catch (StripeException ex) {
            throw new RuntimeException(
                    "Failed to retrieve Stripe session " + checkoutSessionId.value() + ": "
                            + ex.getMessage(),
                    ex);
        }
    }

    private CheckoutSessionStatus mapStatus(String status) {
        return switch (status) {
            case "open" -> CheckoutSessionStatus.OPEN;
            case "complete" -> CheckoutSessionStatus.COMPLETE;
            case "expired" -> CheckoutSessionStatus.EXPIRED;
            default -> {
                log.warn("Unknown Stripe session status: {}", status);
                yield CheckoutSessionStatus.EXPIRED;
            }
        };
    }
}
