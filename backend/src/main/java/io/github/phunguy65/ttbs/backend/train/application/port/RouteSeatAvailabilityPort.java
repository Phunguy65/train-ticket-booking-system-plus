package io.github.phunguy65.ttbs.backend.train.application.port;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;

/**
 * Cross-module port that allows the {@code booking} module to atomically validate and reserve
 * seats on a route.
 *
 * <p>Exposed via the {@code train::port} named interface — the {@code booking} module must
 * declare {@code allowedDependencies = {"train::port", "train::model"}} in its
 * {@code @ApplicationModule}.
 */
public interface RouteSeatAvailabilityPort {

    /**
     * Atomically transitions all specified seats from {@code AVAILABLE} to {@code HELD}.
     * Seat IDs are sorted ascending before locking to prevent deadlocks.
     * All-or-nothing: if any seat is not AVAILABLE, no seats are modified.
     *
     * @param routeId the route
     * @param seatIds the seats to hold
     * @return success if all seats were AVAILABLE and are now HELD;
     * failure with {@link RouteSeatAvailabilityError.SeatNotAvailable} otherwise
     */
    Result<Void, RouteSeatAvailabilityError> holdSeats(RouteId routeId, List<SeatId> seatIds);

    /**
     * Atomically transitions all specified seats from {@code HELD} back to {@code AVAILABLE}.
     * Used when a hold expires or is cancelled.
     *
     * @param routeId the route
     * @param seatIds the seats to release
     * @return success if all seats were released;
     * failure otherwise
     */
    Result<Void, RouteSeatAvailabilityError> releaseHeldSeats(
            RouteId routeId, List<SeatId> seatIds);

    /**
     * Atomically transitions all specified seats from {@code BOOKED} to {@code CANCELLED}.
     * Used when a confirmed booking is cancelled by the user.
     *
     * @param routeId the route
     * @param seatIds the seats to cancel
     * @return success if all seats were cancelled;
     * failure otherwise
     */
    Result<Void, RouteSeatAvailabilityError> cancelBookedSeats(
            RouteId routeId, List<SeatId> seatIds);

    /**
     * Atomically transitions all seats for the given booking from {@code HELD} to {@code BOOKED}.
     * Called after successful payment confirmation.
     *
     * @param bookingId the booking whose held seats should be confirmed
     * @return success if all seats were HELD and are now BOOKED;
     * failure with {@link RouteSeatAvailabilityError.SeatNotAvailable} otherwise
     */
    Result<Void, RouteSeatAvailabilityError> confirmHeldSeats(java.util.UUID bookingId);

    /**
     * Returns the seat IDs associated with the given booking.
     *
     * @param bookingId the booking UUID
     * @return list of seat IDs linked to this booking via {@code route_seat_availability.booking_id}
     */
    List<SeatId> findSeatIdsByBookingId(java.util.UUID bookingId);
}
