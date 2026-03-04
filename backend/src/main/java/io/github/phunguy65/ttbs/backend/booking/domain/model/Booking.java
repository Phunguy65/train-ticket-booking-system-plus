package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingHeld;
import io.github.phunguy65.ttbs.backend.booking.domain.event.SeatHoldExpired;
import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Booking extends AggregateRoot<BookingId> {

    private final BookingId id;
    private final UserId userId;
    private final RouteId routeId;
    private final List<BookedSeat> bookedSeats;
    private final BigDecimal totalPrice;
    private final String currency;
    private final String idempotencyKey;
    private BookingStatus status;
    private final Instant createdAt;
    private String checkoutSessionId;

    private final String passengerName;
    private final String passengerEmail;
    private final String passengerPhone;

    private Booking(
            BookingId id,
            UserId userId,
            RouteId routeId,
            List<BookedSeat> bookedSeats,
            BigDecimal totalPrice,
            String currency,
            String idempotencyKey,
            BookingStatus status,
            Instant createdAt,
            String checkoutSessionId,
            String passengerName,
            String passengerEmail,
            String passengerPhone) {
        this.id = id;
        this.userId = userId;
        this.routeId = routeId;
        this.bookedSeats = Collections.unmodifiableList(bookedSeats);
        this.totalPrice = totalPrice;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
        this.checkoutSessionId = checkoutSessionId;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.passengerPhone = passengerPhone;
    }

    /**
     * Factory method for creating a new seat hold.
     * Registers {@link BookingHeld} domain event.
     *
     * @param userId             the user creating the hold
     * @param routeId            the route for which the hold is created
     * @param bookedSeats        list of seats with price snapshots
     * @param totalPrice         sum of all unit prices
     * @param currency           the currency code (e.g. "VND")
     * @param checkoutSessionId  Stripe Checkout Session ID (set after session creation)
     * @param idempotencyKey     idempotency key for deduplication
     * @param passengerName      passenger name
     * @param passengerEmail     passenger email
     * @param passengerPhone     passenger phone (nullable)
     */
    public static Booking createHold(
            UUID userId,
            UUID routeId,
            List<BookedSeat> bookedSeats,
            BigDecimal totalPrice,
            String currency,
            String checkoutSessionId,
            String idempotencyKey,
            String passengerName,
            String passengerEmail,
            String passengerPhone) {
        BookingId bookingId = BookingId.of(UUID.randomUUID());
        Instant now = Instant.now();
        Booking booking = new Booking(
                bookingId,
                UserId.of(userId),
                RouteId.of(routeId),
                bookedSeats,
                totalPrice,
                currency,
                idempotencyKey,
                BookingStatus.HELD,
                now,
                checkoutSessionId,
                passengerName,
                passengerEmail,
                passengerPhone);
        booking.registerEvent(BookingHeld.of(
                bookingId,
                userId,
                routeId,
                bookedSeats.stream().map(bs -> bs.seatId().value()).toList()));
        return booking;
    }

    /**
     * Factory method for reconstituting a booking from persistence.
     * Does NOT register domain events.
     */
    public static Booking reconstitute(
            UUID id,
            UUID userId,
            UUID routeId,
            List<BookedSeat> bookedSeats,
            BigDecimal totalPrice,
            String currency,
            String idempotencyKey,
            BookingStatus status,
            Instant createdAt,
            String checkoutSessionId,
            String passengerName,
            String passengerEmail,
            String passengerPhone) {
        return new Booking(
                BookingId.of(id),
                UserId.of(userId),
                RouteId.of(routeId),
                bookedSeats,
                totalPrice,
                currency,
                idempotencyKey,
                status,
                createdAt,
                checkoutSessionId,
                passengerName,
                passengerEmail,
                passengerPhone);
    }

    /**
     * Confirms the hold after payment.
     * Guard: status must be HELD.
     *
     * @return success if confirmed; failure with {@link BookingError.InvalidStatusTransition} otherwise
     */
    public Result<Void, BookingError> confirm() {
        if (status != BookingStatus.HELD) {
            return Result.failure(new BookingError.InvalidStatusTransition(status));
        }
        this.status = BookingStatus.CONFIRMED;
        registerEvent(BookingConfirmed.of(id));
        return Result.success();
    }

    /**
     * Expires a HELD booking (deadline passed without confirmation).
     * Transitions HELD → CANCELLED. Registers {@link SeatHoldExpired} event.
     *
     * @return success always (already expired bookings are silently ignored)
     */
    public Result<Void, BookingError> expire() {
        if (status != BookingStatus.HELD) {
            return Result.success();
        }
        this.status = BookingStatus.CANCELLED;
        registerEvent(SeatHoldExpired.of(
                id,
                userId.value(),
                routeId.value(),
                bookedSeats.stream().map(bs -> bs.seatId().value()).toList()));
        return Result.success();
    }

    /**
     * Cancels the booking (user-initiated or system-initiated).
     * Returns failure with {@link BookingError.AlreadyCancelled} if already cancelled.
     */
    public Result<Void, BookingError> cancel() {
        if (status == BookingStatus.CANCELLED) {
            return Result.failure(new BookingError.AlreadyCancelled());
        }
        this.status = BookingStatus.CANCELLED;
        registerEvent(BookingCancelled.of(id, checkoutSessionId));
        return Result.success();
    }

    @Override
    public BookingId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public RouteId getRouteId() {
        return routeId;
    }

    public List<BookedSeat> getBookedSeats() {
        return bookedSeats;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCheckoutSessionId() {
        return checkoutSessionId;
    }

    public void setCheckoutSessionId(String checkoutSessionId) {
        this.checkoutSessionId = checkoutSessionId;
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
}
