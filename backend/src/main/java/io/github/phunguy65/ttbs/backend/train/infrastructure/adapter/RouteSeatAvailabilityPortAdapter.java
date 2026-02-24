package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements {@link RouteSeatAvailabilityPort} using the domain
 * repository.
 *
 * <p>Placed in {@code train.infrastructure.adapter} — the port contract lives in
 * {@code train.domain.model} and is exposed via the {@code train::model} named interface.
 */
@Service
public class RouteSeatAvailabilityPortAdapter implements RouteSeatAvailabilityPort {

    private final RouteSeatAvailabilityRepository repository;

    public RouteSeatAvailabilityPortAdapter(RouteSeatAvailabilityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> reserveSeat(RouteId routeId, SeatId seatId) {
        Optional<RouteSeatAvailability> found = repository.findByRouteIdAndSeatId(routeId, seatId);

        if (found.isEmpty()) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }

        RouteSeatAvailability availability = found.get();
        Result<Void, RouteSeatAvailabilityError> bookResult = availability.book();

        if (bookResult.isFailure()) {
            return bookResult;
        }

        repository.save(availability);
        return Result.success();
    }
}
