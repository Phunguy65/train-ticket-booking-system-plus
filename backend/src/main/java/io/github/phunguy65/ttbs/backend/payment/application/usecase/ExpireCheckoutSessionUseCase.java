package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.payment.application.command.ExpireCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpireCheckoutSessionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpireCheckoutSessionUseCase.class);

    private final PaymentRepository paymentRepository;
    private final StripeGatewayPort stripeGatewayPort;

    public ExpireCheckoutSessionUseCase(
            PaymentRepository paymentRepository, StripeGatewayPort stripeGatewayPort) {
        this.paymentRepository = paymentRepository;
        this.stripeGatewayPort = stripeGatewayPort;
    }

    @Transactional
    public void execute(ExpireCheckoutSessionCommand command) {
        var bookingId = command.bookingId();
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        if (payment == null) {
            log.info("No payment found for bookingId={}, nothing to expire", bookingId);
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info(
                    "Payment for bookingId={} is in status {}, skipping expiry",
                    bookingId,
                    payment.getStatus());
            return;
        }

        stripeGatewayPort.expireCheckoutSession(payment.getCheckoutSessionId());
        payment.markCancelled();
        paymentRepository.save(payment);

        log.info("Checkout session expired for bookingId={}", bookingId);
    }
}
