package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;

/**
 * Domain entity tracking the availability of a specific seat on a specific scheduled trip.
 *
 * <p>This entity is NOT an AggregateRoot — it is managed within the seat management context.
 * Concurrency safety is enforced via JPA {@code @Version} optimistic locking at the database
 * layer. Any concurrent modification will result in an {@code OptimisticLockException}.
 *
 * <p>Allowed state transitions:
 * <ul>
 *   <li>AVAILABLE → HELD (via {@link #hold(java.util.UUID, Money)})
 *   <li>HELD → BOOKED (via {@link #confirmHold()})
 *   <li>HELD → AVAILABLE (via {@link #expire()})
 *   <li>AVAILABLE → BOOKED (via {@link #book(Money)})
 *   <li>BOOKED → CANCELLED (via {@link #cancel()})
 *   <li>CANCELLED → AVAILABLE (via {@link #release()})
 * </ul>
 *
 * <p>Price snapshot semantics:
 * <ul>
 *   <li>{@code hold(bookingId, price)}: captures price at booking time</li>
 *   <li>{@code expire()}: clears price (seat returns to unsold pool)</li>
 *   <li>{@code cancel()}: retains price snapshot for record integrity</li>
 *   <li>{@code release()}: clears price (seat returns to unsold pool)</li>
 *   <li>{@code book(price)}: requires price, retained through BOOKED state</li>
 * </ul>
 */
public class RouteSeatAvailability {

    private final ScheduledTripId scheduledTripId;
    private final SeatId seatId;
    private RouteSeatAvailabilityStatus status;
    private java.util.UUID bookingId;
    private Money priceAtBooking;
    private final Integer version;

    private RouteSeatAvailability(
            ScheduledTripId scheduledTripId,
            SeatId seatId,
            RouteSeatAvailabilityStatus status,
            java.util.UUID bookingId,
            Money priceAtBooking,
            Integer version) {
        this.scheduledTripId = scheduledTripId;
        this.seatId = seatId;
        this.status = status;
        this.bookingId = bookingId;
        this.priceAtBooking = priceAtBooking;
        this.version = version;
    }

    /**
     * Factory method for creating a new availability record with status {@code AVAILABLE}.
     */
    public static RouteSeatAvailability create(ScheduledTripId scheduledTripId, SeatId seatId) {
        return new RouteSeatAvailability(
                scheduledTripId, seatId, RouteSeatAvailabilityStatus.AVAILABLE, null, null, null);
    }

    /**
     * Factory method for reconstituting from persistence.
     */
    public static RouteSeatAvailability reconstitute(
            ScheduledTripId scheduledTripId,
            SeatId seatId,
            RouteSeatAvailabilityStatus status,
            java.util.UUID bookingId) {
        return new RouteSeatAvailability(scheduledTripId, seatId, status, bookingId, null, null);
    }

    /**
     * Factory method for reconstituting from persistence with version.
     */
    public static RouteSeatAvailability reconstitute(
            ScheduledTripId scheduledTripId,
            SeatId seatId,
            RouteSeatAvailabilityStatus status,
            java.util.UUID bookingId,
            Integer version) {
        return new RouteSeatAvailability(scheduledTripId, seatId, status, bookingId, null, version);
    }

    /**
     * Factory method for reconstituting from persistence with price snapshot.
     */
    public static RouteSeatAvailability reconstitute(
            ScheduledTripId scheduledTripId,
            SeatId seatId,
            RouteSeatAvailabilityStatus status,
            java.util.UUID bookingId,
            Money priceAtBooking,
            Integer version) {
        return new RouteSeatAvailability(
                scheduledTripId, seatId, status, bookingId, priceAtBooking, version);
    }

    /**
     * Transitions status from {@code AVAILABLE} to {@code HELD} and captures the price snapshot.
     *
     * @param bookingId the booking ID to associate with this held seat
     * @param price the price snapshot captured at booking time
     * @return success if the seat was AVAILABLE; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> hold(java.util.UUID bookingId, Money price) {
        if (status != RouteSeatAvailabilityStatus.AVAILABLE) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.HELD;
        this.bookingId = bookingId;
        this.priceAtBooking = price;
        return Result.success();
    }

    /**
     * Transitions status from {@code AVAILABLE} to {@code HELD} without capturing a price.
     * Use {@link #hold(java.util.UUID, Money)} to capture the price snapshot.
     *
     * @return success if the seat was AVAILABLE; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> hold() {
        return hold(null, null);
    }

    /**
     * Transitions status from {@code HELD} to {@code BOOKED} after payment confirmation.
     * The price snapshot is retained from the {@link #hold(java.util.UUID, Money)} call.
     *
     * @return success if the seat was HELD; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> confirmHold() {
        if (status != RouteSeatAvailabilityStatus.HELD) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.BOOKED;
        return Result.success();
    }

    /**
     * Transitions status from {@code HELD} back to {@code AVAILABLE} when a hold expires.
     * The price snapshot is cleared.
     *
     * @return success if the seat was HELD; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> expire() {
        if (status != RouteSeatAvailabilityStatus.HELD) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.AVAILABLE;
        this.bookingId = null;
        this.priceAtBooking = null;
        return Result.success();
    }

    /**
     * Transitions status from {@code AVAILABLE} to {@code BOOKED} with a required price snapshot.
     *
     * @param price the price snapshot captured at booking time
     * @return success if the seat was AVAILABLE; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> book(Money price) {
        if (status != RouteSeatAvailabilityStatus.AVAILABLE) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.BOOKED;
        this.priceAtBooking = price;
        return Result.success();
    }

    /**
     * Transitions status from {@code BOOKED} to {@code CANCELLED}.
     * The price snapshot is retained for record integrity.
     *
     * @return success if the seat was BOOKED; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> cancel() {
        if (status != RouteSeatAvailabilityStatus.BOOKED) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.CANCELLED;
        this.bookingId = null;
        return Result.success();
    }

    /**
     * Transitions status from {@code CANCELLED} back to {@code AVAILABLE}.
     * The price snapshot is cleared.
     *
     * @return success if the seat was CANCELLED; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> release() {
        if (status != RouteSeatAvailabilityStatus.CANCELLED) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.AVAILABLE;
        this.priceAtBooking = null;
        return Result.success();
    }

    public ScheduledTripId getScheduledTripId() {
        return scheduledTripId;
    }

    public SeatId getSeatId() {
        return seatId;
    }

    public RouteSeatAvailabilityStatus getStatus() {
        return status;
    }

    public java.util.UUID getBookingId() {
        return bookingId;
    }

    public Money getPriceAtBooking() {
        return priceAtBooking;
    }

    public Integer getVersion() {
        return version;
    }
}
