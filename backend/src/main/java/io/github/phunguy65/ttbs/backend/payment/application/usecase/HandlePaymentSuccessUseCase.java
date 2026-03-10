package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HandlePaymentSuccessUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandlePaymentSuccessUseCase.class);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;
    private final StripeGatewayPort stripeGatewayPort;
    private final ApplicationEventPublisher eventPublisher;

    public HandlePaymentSuccessUseCase(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            RouteSeatAvailabilityPort seatAvailabilityPort,
            StripeGatewayPort stripeGatewayPort,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.stripeGatewayPort = stripeGatewayPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(
            String checkoutSessionId, String stripePaymentIntentId, String stripeEventId) {

        if (paymentRepository.findByStripeEventId(stripeEventId).isPresent()) {
            log.info("Stripe event {} already processed, skipping", stripeEventId);
            return;
        }

        Payment payment = paymentRepository
                .findByCheckoutSessionId(checkoutSessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "No payment found for checkoutSessionId=" + checkoutSessionId));

        BookingId bookingId = payment.getBookingId();
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalStateException("No booking found for bookingId=" + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn(
                    "Payment arrived after booking expiry for bookingId={}, issuing immediate refund",
                    bookingId);
            stripeGatewayPort.createRefund(stripePaymentIntentId, "refund_" + bookingId.value());
            payment.markRefunded();
            paymentRepository.save(payment);
            return;
        }

        // Happy path: confirm booking and seats
        var confirmResult = booking.confirm();
        if (confirmResult.isFailure()) {
            log.error(
                    "Failed to confirm booking {} during payment success: {}",
                    bookingId,
                    confirmResult);
            throw new IllegalStateException(
                    "Cannot confirm booking " + bookingId + ": " + confirmResult);
        }
        bookingRepository.save(booking);

        // Publish domain events from booking aggregate
        booking.getDomainEvents().forEach(eventPublisher::publishEvent);
        booking.clearDomainEvents();

        // Transition seats HELD → BOOKED
        var seatResult = seatAvailabilityPort.confirmHeldSeats(bookingId.value());
        if (seatResult.isFailure()) {
            log.error("Failed to confirm seats for bookingId={}: {}", bookingId, seatResult);
            throw new IllegalStateException("Cannot confirm seats for booking " + bookingId);
        }

        // Mark payment as PAID
        payment.markPaid(stripePaymentIntentId, stripeEventId);
        paymentRepository.save(payment);

        // Publish payment domain events
        payment.getDomainEvents().forEach(eventPublisher::publishEvent);
        payment.clearDomainEvents();

        log.info("Payment success processed for bookingId={}", bookingId);
    }
}
