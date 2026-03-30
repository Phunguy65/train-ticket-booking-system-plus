package io.github.phunguy65.ttbs.backend.train.domain.model;

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
 *   <li>AVAILABLE → HELD (via {@link #hold()})
 *   <li>HELD → BOOKED (via {@link #confirmHold()})
 *   <li>HELD → AVAILABLE (via {@link #expire()})
 *   <li>AVAILABLE → BOOKED (via {@link #book()})
 *   <li>BOOKED → CANCELLED (via {@link #cancel()})
 *   <li>CANCELLED → AVAILABLE (via {@link #release()})
 * </ul>
 */
public class RouteSeatAvailability {

    private final ScheduledTripId scheduledTripId;
    private final SeatId seatId;
    private RouteSeatAvailabilityStatus status;
    private java.util.UUID bookingId;
    private final Integer version;

    private RouteSeatAvailability(
            ScheduledTripId scheduledTripId,
            SeatId seatId,
            RouteSeatAvailabilityStatus status,
            java.util.UUID bookingId,
            Integer version) {
        this.scheduledTripId = scheduledTripId;
        this.seatId = seatId;
        this.status = status;
        this.bookingId = bookingId;
        this.version = version;
    }

    /**
     * Factory method for creating a new availability record with status {@code AVAILABLE}.
     */
    public static RouteSeatAvailability create(ScheduledTripId scheduledTripId, SeatId seatId) {
        return new RouteSeatAvailability(
                scheduledTripId, seatId, RouteSeatAvailabilityStatus.AVAILABLE, null, null);
    }

    /**
     * Factory method for reconstituting from persistence.
     */
    public static RouteSeatAvailability reconstitute(
            ScheduledTripId scheduledTripId,
            SeatId seatId,
            RouteSeatAvailabilityStatus status,
            java.util.UUID bookingId) {
        return new RouteSeatAvailability(scheduledTripId, seatId, status, bookingId, null);
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
        return new RouteSeatAvailability(scheduledTripId, seatId, status, bookingId, version);
    }

    /**
     * Transitions status from {@code AVAILABLE} to {@code HELD}.
     *
     * @return success if the seat was AVAILABLE; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> hold(java.util.UUID bookingId) {
        if (status != RouteSeatAvailabilityStatus.AVAILABLE) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.HELD;
        this.bookingId = bookingId;
        return Result.success();
    }

    /**
     * Transitions status from {@code AVAILABLE} to {@code HELD}.
     * Booking ID is set separately via {@link #setBookingId(java.util.UUID)}.
     *
     * @return success if the seat was AVAILABLE; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> hold() {
        return hold(null);
    }

    /**
     * Sets the booking ID associated with this seat when it is HELD or BOOKED.
     */
    public void setBookingId(java.util.UUID bookingId) {
        this.bookingId = bookingId;
    }

    /**
     * Transitions status from {@code HELD} to {@code BOOKED} after payment confirmation.
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
     *
     * @return success if the seat was HELD; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> expire() {
        if (status != RouteSeatAvailabilityStatus.HELD) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.AVAILABLE;
        this.bookingId = null;
        return Result.success();
    }

    /**
     * Transitions status from {@code AVAILABLE} to {@code BOOKED}.
     *
     * @return success if the seat was AVAILABLE; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> book() {
        if (status != RouteSeatAvailabilityStatus.AVAILABLE) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.BOOKED;
        return Result.success();
    }

    /**
     * Transitions status from {@code BOOKED} to {@code CANCELLED}.
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
     *
     * @return success if the seat was CANCELLED; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> release() {
        if (status != RouteSeatAvailabilityStatus.CANCELLED) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.AVAILABLE;
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

    public Integer getVersion() {
        return version;
    }
}
