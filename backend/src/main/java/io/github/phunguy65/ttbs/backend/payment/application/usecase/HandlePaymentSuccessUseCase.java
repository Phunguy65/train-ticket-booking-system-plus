package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.command.HandlePaymentSuccessCommand;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.event.SeatStatusChangedEvent;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import java.time.Instant;
import java.util.List;
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
    private final RouteSeatAvailabilityManager seatAvailabilityPort;
    private final StripeGatewayPort stripeGatewayPort;
    private final ApplicationEventPublisher eventPublisher;

    public HandlePaymentSuccessUseCase(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            RouteSeatAvailabilityManager seatAvailabilityPort,
            StripeGatewayPort stripeGatewayPort,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.stripeGatewayPort = stripeGatewayPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(HandlePaymentSuccessCommand command) {

        if (paymentRepository.findByStripeEventId(command.stripeEventId()).isPresent()) {
            log.info("Stripe event {} already processed, skipping", command.stripeEventId());
            return;
        }

        Payment payment = paymentRepository
                .findByCheckoutSessionId(command.checkoutSessionId())
                .orElseThrow(() -> new IllegalStateException(
                        "No payment found for checkoutSessionId=" + command.checkoutSessionId()));

        BookingId bookingId = payment.getBookingId();
        Booking booking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() ->
                        new IllegalStateException("No booking found for bookingId=" + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn(
                    "Payment arrived after booking expiry for bookingId={}, issuing immediate refund",
                    bookingId);
            stripeGatewayPort.createRefund(
                    command.stripePaymentIntentId(), "refund_" + bookingId.value());
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
        payment.markPaid(command.stripePaymentIntentId(), command.stripeEventId());
        paymentRepository.save(payment);

        // Publish payment domain events
        payment.getDomainEvents().forEach(eventPublisher::publishEvent);
        payment.clearDomainEvents();

        List<RouteSeatAvailability> confirmedSeats =
                seatAvailabilityPort.findByBookingId(bookingId.value());
        if (!confirmedSeats.isEmpty()) {
            List<SeatStatusChangedEvent.SeatChange> changes = confirmedSeats.stream()
                    .map(seat -> new SeatStatusChangedEvent.SeatChange(
                            seat.getSeatId().value(), seat.getStatus().name(), bookingId.value()))
                    .toList();
            SeatStatusChangedEvent sseEvent = new SeatStatusChangedEvent(
                    booking.getScheduledTripId().value(), changes, Instant.now());
            eventPublisher.publishEvent(sseEvent);
        }

        log.info("Payment success processed for bookingId={}", bookingId);
    }
}
