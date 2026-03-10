package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandlePaymentFailedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandlePaymentFailedUseCase.class);

    private final PaymentRepository paymentRepository;

    public HandlePaymentFailedUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void execute(String checkoutSessionId, String errorMessage, String stripeEventId) {
        if (paymentRepository.findByStripeEventId(stripeEventId).isPresent()) {
            log.info("Stripe event {} already processed, skipping", stripeEventId);
            return;
        }

        paymentRepository
                .findByCheckoutSessionId(checkoutSessionId)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .ifPresentOrElse(
                        p -> {
                            p.markFailed(errorMessage, stripeEventId);
                            paymentRepository.save(p);
                            log.info(
                                    "Payment marked FAILED for checkoutSessionId={}",
                                    checkoutSessionId);
                        },
                        () -> log.warn(
                                "No PENDING payment found for checkoutSessionId={}",
                                checkoutSessionId));
    }

    /**
     * Handles payment failure when only the payment intent ID is available (from webhook).
     * Looks up the payment by stripePaymentIntentId.
     */
    @Transactional
    public void executeByPaymentIntent(
            String stripePaymentIntentId, String errorMessage, String stripeEventId) {
        if (paymentRepository.findByStripeEventId(stripeEventId).isPresent()) {
            log.info("Stripe event {} already processed, skipping", stripeEventId);
            return;
        }

        paymentRepository
                .findByStripePaymentIntentId(stripePaymentIntentId)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING
                        || p.getStatus() == PaymentStatus.PAID)
                .ifPresentOrElse(
                        p -> {
                            p.markFailed(errorMessage, stripeEventId);
                            paymentRepository.save(p);
                            log.info(
                                    "Payment marked FAILED for paymentIntentId={}",
                                    stripePaymentIntentId);
                        },
                        () -> log.warn(
                                "No payment found for paymentIntentId={}", stripePaymentIntentId));
    }
}
