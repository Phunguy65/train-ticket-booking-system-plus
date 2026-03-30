package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.payment.application.command.HandlePaymentFailedByPaymentIntentCommand;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandlePaymentFailedByPaymentIntentUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(HandlePaymentFailedByPaymentIntentUseCase.class);

    private final PaymentRepository paymentRepository;

    public HandlePaymentFailedByPaymentIntentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void execute(HandlePaymentFailedByPaymentIntentCommand command) {
        if (paymentRepository.findByStripeEventId(command.stripeEventId()).isPresent()) {
            log.info("Stripe event {} already processed, skipping", command.stripeEventId());
            return;
        }

        paymentRepository
                .findByStripePaymentIntentId(command.stripePaymentIntentId())
                .filter(p -> p.getStatus() == PaymentStatus.PENDING
                        || p.getStatus() == PaymentStatus.PAID)
                .ifPresentOrElse(
                        p -> {
                            p.markFailed(command.errorMessage(), command.stripeEventId());
                            paymentRepository.save(p);
                            log.info(
                                    "Payment marked FAILED for paymentIntentId={}",
                                    command.stripePaymentIntentId());
                        },
                        () -> log.warn(
                                "No payment found for paymentIntentId={}",
                                command.stripePaymentIntentId()));
    }
}
