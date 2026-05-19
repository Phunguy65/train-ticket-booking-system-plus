package io.github.phunguy65.ttbs.backend.booking.application.helper;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.port.StripeGatewayPort;
import io.github.phunguy65.ttbs.backend.payment.domain.model.Payment;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Helper service that forcefully cancels a booking (bypassing user-ownership checks),
 * releases associated seat availability, and issues a refund when applicable.
 *
 * <p>Intended for internal orchestration use — e.g. when an admin deletes a seat that
 * still has active bookings. This service does NOT open its own transaction; callers
 * are expected to run within an existing {@code @Transactional} context.
 */
@Service
public class ForceBookingCancellationHelper {

    private static final Logger log = LoggerFactory.getLogger(ForceBookingCancellationHelper.class);

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityManager seatAvailabilityManager;
    private final PaymentRepository paymentRepository;
    private final StripeGatewayPort stripeGatewayPort;
    private final ApplicationEventPublisher eventPublisher;

    public ForceBookingCancellationHelper(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityManager seatAvailabilityManager,
            PaymentRepository paymentRepository,
            StripeGatewayPort stripeGatewayPort,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityManager = seatAvailabilityManager;
        this.paymentRepository = paymentRepository;
        this.stripeGatewayPort = stripeGatewayPort;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Cancels the booking identified by {@code bookingId}, releases its seat availability
     * records, publishes domain events, and issues a Stripe refund if the booking was
     * already CONFIRMED (i.e. payment was captured).
     *
     * <p>If the booking is already CANCELLED this method is a no-op (idempotent).
     * If the booking is not found, the call is silently skipped with a warning log.
     *
     * @param bookingId the UUID of the booking to cancel
     */
    public void cancel(UUID bookingId) {
        Booking booking = bookingRepository.findById(BookingId.of(bookingId)).orElse(null);
        if (booking == null) {
            log.warn("ForceBookingCancellation: booking {} not found, skipping", bookingId);
            return;
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.debug(
                    "ForceBookingCancellation: booking {} already CANCELLED, skipping", bookingId);
            return;
        }

        BookingStatus previousStatus = booking.getStatus();

        var cancelResult = booking.cancel();
        if (cancelResult.isFailure()) {
            log.warn(
                    "ForceBookingCancellation: could not cancel booking {}: {}",
                    bookingId,
                    cancelResult);
            return;
        }

        List<SeatId> seatIds = seatAvailabilityManager.findSeatIdsByBookingId(bookingId);
        if (!seatIds.isEmpty()) {
            if (previousStatus == BookingStatus.HELD) {
                seatAvailabilityManager.releaseHeldSeats(booking.getScheduledTripId(), seatIds);
            } else if (previousStatus == BookingStatus.CONFIRMED) {
                seatAvailabilityManager.cancelBookedSeats(booking.getScheduledTripId(), seatIds);
            }
        }

        bookingRepository.save(booking);

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        if (previousStatus == BookingStatus.CONFIRMED) {
            issueRefund(BookingId.of(bookingId));
        }
    }

    private void issueRefund(BookingId bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);

        if (payment == null) {
            log.info(
                    "ForceBookingCancellation: no payment found for bookingId={}, skipping refund",
                    bookingId);
            return;
        }

        if (payment.getStatus() != PaymentStatus.PAID) {
            log.info(
                    "ForceBookingCancellation: payment for bookingId={} is in status {}, skipping"
                            + " refund",
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

            log.info("ForceBookingCancellation: refund issued for bookingId={}", bookingId);
        } catch (Exception e) {
            log.error(
                    "ForceBookingCancellation: refund failed for bookingId={}, paymentIntentId={}:"
                            + " {}",
                    bookingId,
                    payment.getStripePaymentIntentId(),
                    e.getMessage(),
                    e);
        }
    }
}
