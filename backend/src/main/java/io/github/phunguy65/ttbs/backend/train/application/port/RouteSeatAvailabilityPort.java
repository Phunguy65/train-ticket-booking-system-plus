package io.github.phunguy65.ttbs.backend.train.application.port;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;

/**
 * Cross-module port that allows the {@code booking} module to atomically validate and reserve
 * a seat on a route.
 *
 * <p>Exposed via the {@code train::port} named interface — the {@code booking} module must
 * declare {@code allowedDependencies = {"train::port", "train::model"}} in its
 * {@code @ApplicationModule}.
 */
public interface RouteSeatAvailabilityPort {

    /**
     * Atomically marks the seat as {@code BOOKED} for the given route.
     *
     * @param routeId the route on which to reserve the seat
     * @param seatId the seat to reserve
     * @return success if the seat was AVAILABLE and is now BOOKED;
     *         failure with {@link RouteSeatAvailabilityError.SeatNotAvailable} otherwise
     */
    Result<Void, RouteSeatAvailabilityError> reserveSeat(RouteId routeId, SeatId seatId);
}
