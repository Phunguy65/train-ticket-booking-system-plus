package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.time.Instant;

/**
 * Aggregate root representing a train ticket booking.
 *
 * <p>Status machine:
 * <ul>
 *   <li>HELD → CONFIRMED (via {@link #confirm()})
 *   <li>HELD → CANCELLED (via {@link #cancel()})
 *   <li>CONFIRMED → CANCELLED (via {@link #cancel()})
 * </ul>
 */
public class Booking extends AggregateRoot<BookingId> {

    private final BookingId bookingId;
    private final UserId userId;
    private final RouteId routeId;
    private final String passengerName;
    private final String passengerEmail;
    private final String passengerPhone;
    private final Money totalPrice;
    private final String currency;
    private BookingStatus status;
    private final String idempotencyKey;
    private final Instant paymentDeadline;
    private final Instant createdAt;

    private Booking(
            BookingId bookingId,
            UserId userId,
            RouteId routeId,
            String passengerName,
            String passengerEmail,
            String passengerPhone,
            Money totalPrice,
            String currency,
            BookingStatus status,
            String idempotencyKey,
            Instant paymentDeadline,
            Instant createdAt) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.routeId = routeId;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.passengerPhone = passengerPhone;
        this.totalPrice = totalPrice;
        this.currency = currency;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.paymentDeadline = paymentDeadline;
        this.createdAt = createdAt;
    }

    /**
     * Creates a new booking in HELD status and registers a {@link BookingCreated} event.
     */
    public static Booking create(
            BookingId bookingId,
            UserId userId,
            RouteId routeId,
            String passengerName,
            String passengerEmail,
            String passengerPhone,
            Money totalPrice,
            String currency,
            String idempotencyKey,
            Instant paymentDeadline) {
        Booking booking = new Booking(
                bookingId,
                userId,
                routeId,
                passengerName,
                passengerEmail,
                passengerPhone,
                totalPrice,
                currency,
                BookingStatus.HELD,
                idempotencyKey,
                paymentDeadline,
                Instant.now());
        booking.registerEvent(new BookingCreated(bookingId, userId, routeId));
        return booking;
    }

    /**
     * Reconstitutes a booking from persistence — no events registered.
     */
    public static Booking reconstitute(
            BookingId bookingId,
            UserId userId,
            RouteId routeId,
            String passengerName,
            String passengerEmail,
            String passengerPhone,
            Money totalPrice,
            String currency,
            BookingStatus status,
            String idempotencyKey,
            Instant paymentDeadline,
            Instant createdAt) {
        return new Booking(
                bookingId,
                userId,
                routeId,
                passengerName,
                passengerEmail,
                passengerPhone,
                totalPrice,
                currency,
                status,
                idempotencyKey,
                paymentDeadline,
                createdAt);
    }

    /**
     * Transitions status from HELD to CONFIRMED and registers a {@link BookingConfirmed} event.
     *
     * @return success if status was HELD; failure with {@link BookingError.InvalidStatusTransition} otherwise
     */
    public Result<Void, BookingError> confirm() {
        if (status != BookingStatus.HELD) {
            return Result.failure(new BookingError.InvalidStatusTransition(
                    status.name(), BookingStatus.CONFIRMED.name()));
        }
        this.status = BookingStatus.CONFIRMED;
        registerEvent(new BookingConfirmed(bookingId, userId, routeId));
        return Result.success();
    }

    /**
     * Transitions status from HELD or CONFIRMED to CANCELLED and registers a
     * {@link BookingCancelled} event. The event carries {@code requiresRefund=true} when
     * cancelling a CONFIRMED booking.
     *
     * @return success if status was HELD or CONFIRMED;
     *         failure with {@link BookingError.InvalidStatusTransition} if already CANCELLED
     */
    public Result<Void, BookingError> cancel() {
        if (status == BookingStatus.CANCELLED) {
            return Result.failure(new BookingError.InvalidStatusTransition(
                    status.name(), BookingStatus.CANCELLED.name()));
        }
        boolean requiresRefund = (status == BookingStatus.CONFIRMED);
        this.status = BookingStatus.CANCELLED;
        registerEvent(new BookingCancelled(bookingId, userId, routeId, requiresRefund));
        return Result.success();
    }

    @Override
    public BookingId getId() {
        return bookingId;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public UserId getUserId() {
        return userId;
    }

    public RouteId getRouteId() {
        return routeId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getPassengerEmail() {
        return passengerEmail;
    }

    public String getPassengerPhone() {
        return passengerPhone;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getPaymentDeadline() {
        return paymentDeadline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
