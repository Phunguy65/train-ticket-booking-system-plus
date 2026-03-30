package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.github.phunguy65.ttbs.backend.payment.application.command.CancelPendingPaymentCommand;
import io.github.phunguy65.ttbs.backend.payment.application.command.HandlePaymentFailedByPaymentIntentCommand;
import io.github.phunguy65.ttbs.backend.payment.application.command.HandlePaymentSuccessCommand;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.CancelPendingPaymentUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.HandlePaymentFailedByPaymentIntentUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.HandlePaymentSuccessUseCase;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.stripe.StripeConfig;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeConfig stripeConfig;
    private final HandlePaymentSuccessUseCase handlePaymentSuccessUseCase;
    private final HandlePaymentFailedByPaymentIntentUseCase
            handlePaymentFailedByPaymentIntentUseCase;
    private final CancelPendingPaymentUseCase cancelPendingPaymentUseCase;

    StripeWebhookController(
            StripeConfig stripeConfig,
            HandlePaymentSuccessUseCase handlePaymentSuccessUseCase,
            HandlePaymentFailedByPaymentIntentUseCase handlePaymentFailedByPaymentIntentUseCase,
            CancelPendingPaymentUseCase cancelPendingPaymentUseCase) {
        this.stripeConfig = stripeConfig;
        this.handlePaymentSuccessUseCase = handlePaymentSuccessUseCase;
        this.handlePaymentFailedByPaymentIntentUseCase = handlePaymentFailedByPaymentIntentUseCase;
        this.cancelPendingPaymentUseCase = cancelPendingPaymentUseCase;
    }

    @PostMapping(value = "/{version}/webhooks/stripe", version = "1.0")
    ResponseEntity<JsendResponse<?>> handleWebhook(
            @RequestHeader("Stripe-Signature") String sigHeader, HttpServletRequest request)
            throws IOException {

        String payload =
                new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(JsendResponse.fail("Invalid webhook signature"));
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new IllegalStateException("Missing session data"));
                    handlePaymentSuccessUseCase.execute(new HandlePaymentSuccessCommand(
                            session.getId(), session.getPaymentIntent(), event.getId()));
                }
                case "checkout.session.expired" -> {
                    Session session = (Session) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(() -> new IllegalStateException("Missing session data"));
                    cancelPendingPaymentUseCase.execute(
                            new CancelPendingPaymentCommand(session.getId()));
                }
                case "payment_intent.payment_failed" -> {
                    PaymentIntent pi = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject()
                            .orElseThrow(
                                    () -> new IllegalStateException("Missing payment intent data"));
                    String errorMsg = pi.getLastPaymentError() != null
                            ? pi.getLastPaymentError().getMessage()
                            : "Payment failed";
                    handlePaymentFailedByPaymentIntentUseCase.execute(
                            new HandlePaymentFailedByPaymentIntentCommand(
                                    pi.getId(), errorMsg, event.getId()));
                }
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }
        } catch (IllegalStateException e) {
            log.warn("Malformed Stripe webhook event {}: {}", event.getId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(JsendResponse.fail("Malformed webhook event: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing Stripe webhook event {}", event.getId(), e);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(JsendResponse.error("Webhook processing failed. Will retry."));
        }

        return ResponseEntity.ok(JsendResponse.success());
    }
}
