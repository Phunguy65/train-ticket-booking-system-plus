package io.github.phunguy65.ttbs.backend.train.domain.model;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;

/**
 * Domain entity tracking the availability of a specific seat on a specific route.
 *
 * <p>This entity is NOT an AggregateRoot — it is managed within the seat management context.
 * Concurrency safety is enforced exclusively via pessimistic locking ({@code SELECT FOR UPDATE
 * NOWAIT}) at the database layer — no optimistic locking ({@code @Version}) is used.
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

    private final RouteId routeId;
    private final SeatId seatId;
    private RouteSeatAvailabilityStatus status;

    private RouteSeatAvailability(
            RouteId routeId, SeatId seatId, RouteSeatAvailabilityStatus status) {
        this.routeId = routeId;
        this.seatId = seatId;
        this.status = status;
    }

    /**
     * Factory method for creating a new availability record with status {@code AVAILABLE}.
     */
    public static RouteSeatAvailability create(RouteId routeId, SeatId seatId) {
        return new RouteSeatAvailability(routeId, seatId, RouteSeatAvailabilityStatus.AVAILABLE);
    }

    /**
     * Factory method for reconstituting from persistence.
     */
    public static RouteSeatAvailability reconstitute(
            RouteId routeId, SeatId seatId, RouteSeatAvailabilityStatus status) {
        return new RouteSeatAvailability(routeId, seatId, status);
    }

    /**
     * Transitions status from {@code AVAILABLE} to {@code HELD}.
     *
     * @return success if the seat was AVAILABLE; failure with {@code SeatNotAvailable} otherwise
     */
    public Result<Void, RouteSeatAvailabilityError> hold() {
        if (status != RouteSeatAvailabilityStatus.AVAILABLE) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }
        this.status = RouteSeatAvailabilityStatus.HELD;
        return Result.success();
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

    public RouteId getRouteId() {
        return routeId;
    }

    public SeatId getSeatId() {
        return seatId;
    }

    public RouteSeatAvailabilityStatus getStatus() {
        return status;
    }
}
