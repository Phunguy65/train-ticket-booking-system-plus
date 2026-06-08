package io.github.phunguy65.ttbs.backend.payment.application;

import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingCancelledPaymentListener {

    private static final Logger log =
            LoggerFactory.getLogger(BookingCancelledPaymentListener.class);

    private final PaymentRepository paymentRepository;

    public BookingCancelledPaymentListener(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelled event) {
        Payment payment = paymentRepository.findByBookingId(event.bookingId()).orElse(null);

        if (payment == null) {
            log.info(
                    "No payment found for cancelled bookingId={}, nothing to cancel",
                    event.bookingId());
            return;
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info(
                    "Payment for cancelled bookingId={} is in status {}, skipping cancellation",
                    event.bookingId(),
                    payment.getStatus());
            return;
        }

        payment.markCancelled();
        paymentRepository.save(payment);

        log.info("Pending payment cancelled for bookingId={}", event.bookingId());
    }
}
