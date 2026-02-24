package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Booking extends AggregateRoot<BookingId> {

    private final BookingId id;
    private final UserId userId;
    private final RouteId routeId;
    private final SeatId seatId;
    private final BigDecimal totalPrice;
    private final String currency;
    private final String idempotencyKey;
    private BookingStatus status;
    private final Instant createdAt;

    private Booking(
            BookingId id,
            UserId userId,
            RouteId routeId,
            SeatId seatId,
            BigDecimal totalPrice,
            String currency,
            String idempotencyKey,
            BookingStatus status,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.routeId = routeId;
        this.seatId = seatId;
        this.totalPrice = totalPrice;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Factory method for creating a new booking. Registers BookingCreated domain event.
     */
    public static Booking create(
            UUID userId,
            UUID routeId,
            UUID seatId,
            BigDecimal totalPrice,
            String currency,
            String idempotencyKey) {
        BookingId bookingId = BookingId.generate();
        Booking booking = new Booking(
                bookingId,
                UserId.of(userId),
                RouteId.of(routeId),
                SeatId.of(seatId),
                totalPrice,
                currency,
                idempotencyKey,
                BookingStatus.PENDING,
                Instant.now());
        booking.registerEvent(BookingCreated.of(bookingId, userId, routeId, seatId));
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
            UUID seatId,
            BigDecimal totalPrice,
            String currency,
            String idempotencyKey,
            BookingStatus status,
            Instant createdAt) {
        return new Booking(
                BookingId.of(id),
                UserId.of(userId),
                RouteId.of(routeId),
                SeatId.of(seatId),
                totalPrice,
                currency,
                idempotencyKey,
                status,
                createdAt);
    }

    public Result<Void, BookingError> confirm() {
        if (status != BookingStatus.PENDING) {
            return Result.failure(new BookingError.CannotConfirm(status));
        }
        this.status = BookingStatus.CONFIRMED;
        registerEvent(BookingConfirmed.of(id));
        return Result.success();
    }

    public Result<Void, BookingError> cancel() {
        if (status == BookingStatus.CANCELLED) {
            return Result.failure(new BookingError.AlreadyCancelled());
        }
        this.status = BookingStatus.CANCELLED;
        registerEvent(BookingCancelled.of(id));
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

    public SeatId getSeatId() {
        return seatId;
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
}
