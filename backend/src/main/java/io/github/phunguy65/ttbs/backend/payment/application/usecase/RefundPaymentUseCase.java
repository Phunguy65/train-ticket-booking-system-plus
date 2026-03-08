package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefundPaymentUseCase.class);

    private final PaymentRepository paymentRepository;
    private final StripeGatewayPort stripeGatewayPort;
    private final ApplicationEventPublisher eventPublisher;

    public RefundPaymentUseCase(
            PaymentRepository paymentRepository,
            StripeGatewayPort stripeGatewayPort,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.stripeGatewayPort = stripeGatewayPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(BookingId bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        if (payment == null) {
            log.info("No payment found for bookingId={}, nothing to refund", bookingId);
            return;
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            log.info(
                    "Payment for bookingId={} is in status {}, skipping refund",
                    bookingId,
                    payment.getStatus());
            return;
        }

        String idempotencyKey = "refund_" + bookingId.value();
        try {
            stripeGatewayPort.createRefund(payment.getStripePaymentIntentId(), idempotencyKey);
            payment.markRefunded();
            paymentRepository.save(payment);

            payment.getDomainEvents().forEach(eventPublisher::publishEvent);
            payment.clearDomainEvents();

            log.info("Refund issued for bookingId={}", bookingId);
        } catch (Exception e) {
            log.error(
                    "Refund failed for bookingId={}, paymentIntentId={}: {}",
                    bookingId,
                    payment.getStripePaymentIntentId(),
                    e.getMessage(),
                    e);
        }
    }
}
