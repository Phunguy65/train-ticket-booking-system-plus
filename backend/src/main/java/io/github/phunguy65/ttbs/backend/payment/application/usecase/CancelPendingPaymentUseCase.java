package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.payment.application.command.CancelPendingPaymentCommand;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels a pending payment identified by its Stripe checkout session ID.
 *
 * <p>Used by the Stripe webhook handler when a {@code checkout.session.expired} event is received.
 */
@Service
public class CancelPendingPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelPendingPaymentUseCase.class);

    private final PaymentRepository paymentRepository;

    public CancelPendingPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public void execute(CancelPendingPaymentCommand command) {
        paymentRepository
                .findByCheckoutSessionId(command.checkoutSessionId())
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .ifPresent(p -> {
                    p.markCancelled();
                    paymentRepository.save(p);
                    log.info(
                            "Payment cancelled via Stripe expiry for session={}",
                            command.checkoutSessionId());
                });
    }
}
