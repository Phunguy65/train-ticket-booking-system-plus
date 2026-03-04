package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelBookingOnExpiryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelBookingOnExpiryUseCase.class);

    private final PaymentRepository paymentRepository;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public CancelBookingOnExpiryUseCase(
            PaymentRepository paymentRepository,
            CancelBookingUseCase cancelBookingUseCase,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(UUID bookingId, String stripeEventId) {
        Optional<Payment> found = paymentRepository.findByBookingId(bookingId);
        if (found.isEmpty()) {
            log.warn("No payment found for bookingId={}, skipping cancel", bookingId);
            return;
        }

        Payment payment = found.get();
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info(
                    "Payment already processed for bookingId={}, status={}, skipping",
                    bookingId,
                    payment.getStatus());
            return;
        }
        var result = payment.cancel(stripeEventId);
        if (result.isFailure()) {
            log.info("Payment cancel failed for bookingId={}, skipping", bookingId);
            return;
        }

        paymentRepository.save(payment);

        for (DomainEvent event : payment.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        payment.clearDomainEvents();

        cancelBookingUseCase.execute(new CancelBookingCommand(bookingId));
    }
}
