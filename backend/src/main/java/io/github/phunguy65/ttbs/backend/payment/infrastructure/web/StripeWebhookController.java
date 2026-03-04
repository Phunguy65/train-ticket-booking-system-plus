package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.CancelBookingOnExpiryUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.ConfirmBookingOnPaymentUseCase;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.config.StripeProperties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/{version}/webhooks/stripe")
class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private static final String EVENT_SESSION_COMPLETED = "checkout.session.completed";
    private static final String EVENT_SESSION_EXPIRED = "checkout.session.expired";

    private final StripeProperties stripeProperties;
    private final ConfirmBookingOnPaymentUseCase confirmBookingOnPaymentUseCase;
    private final CancelBookingOnExpiryUseCase cancelBookingOnExpiryUseCase;

    StripeWebhookController(
            StripeProperties stripeProperties,
            ConfirmBookingOnPaymentUseCase confirmBookingOnPaymentUseCase,
            CancelBookingOnExpiryUseCase cancelBookingOnExpiryUseCase) {
        this.stripeProperties = stripeProperties;
        this.confirmBookingOnPaymentUseCase = confirmBookingOnPaymentUseCase;
        this.cancelBookingOnExpiryUseCase = cancelBookingOnExpiryUseCase;
    }

    @PostMapping(version = "1.0", consumes = "application/json")
    ResponseEntity<Void> handleWebhook(
            @RequestBody byte[] payload, @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(
                    new String(payload), sigHeader, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException ex) {
            log.warn("Invalid Stripe webhook signature: {}", ex.getMessage());
            return ResponseEntity.badRequest().build();
        }

        String eventType = event.getType();
        log.debug("Received Stripe event: type={}, id={}", eventType, event.getId());

        switch (eventType) {
            case EVENT_SESSION_COMPLETED -> handleSessionCompleted(event);
            case EVENT_SESSION_EXPIRED -> handleSessionExpired(event);
            default -> log.debug("Unhandled Stripe event type: {}", eventType);
        }

        return ResponseEntity.ok().build();
    }

    private void handleSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() ->
                        new IllegalStateException("Could not deserialize session from event"));

        if (!"paid".equals(session.getPaymentStatus())) {
            log.info(
                    "Skipping checkout.session.completed with payment_status={} for session={}",
                    session.getPaymentStatus(),
                    session.getId());
            return;
        }

        String bookingIdStr = session.getMetadata().get("booking_id");
        if (bookingIdStr == null) {
            log.warn(
                    "checkout.session.completed event missing booking_id metadata, session={}",
                    session.getId());
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);
        confirmBookingOnPaymentUseCase.execute(bookingId, event.getId());
    }

    private void handleSessionExpired(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() ->
                        new IllegalStateException("Could not deserialize session from event"));

        String bookingIdStr = session.getMetadata().get("booking_id");
        if (bookingIdStr == null) {
            log.warn(
                    "checkout.session.expired event missing booking_id metadata, session={}",
                    session.getId());
            return;
        }

        UUID bookingId = UUID.fromString(bookingIdStr);
        cancelBookingOnExpiryUseCase.execute(bookingId, event.getId());
    }
}
