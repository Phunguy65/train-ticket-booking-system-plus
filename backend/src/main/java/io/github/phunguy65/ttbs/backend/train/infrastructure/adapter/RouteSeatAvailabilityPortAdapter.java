package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements {@link RouteSeatAvailabilityPort} using the domain
 * repository and pessimistic locking for batch hold operations.
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

    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> holdSeats(
            RouteId routeId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> locked =
                repository.findByRouteIdAndSeatIdsForUpdate(routeId, seatIds);

        if (locked.size() != seatIds.size()) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : locked) {
            Result<Void, RouteSeatAvailabilityError> holdResult = domain.hold();
            if (holdResult.isFailure()) {
                return holdResult;
            }
            toSave.add(domain);
        }

        repository.saveAll(toSave);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> confirmHeldSeats(
            RouteId routeId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> locked =
                repository.findByRouteIdAndSeatIdsForUpdate(routeId, seatIds);

        if (locked.size() != seatIds.size()) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : locked) {
            Result<Void, RouteSeatAvailabilityError> confirmResult = domain.confirmHold();
            if (confirmResult.isFailure()) {
                return confirmResult;
            }
            toSave.add(domain);
        }

        repository.saveAll(toSave);
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> releaseHeldSeats(
            RouteId routeId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> locked =
                repository.findByRouteIdAndSeatIdsForUpdate(routeId, seatIds);

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : locked) {
            if (domain.getStatus() == RouteSeatAvailabilityStatus.HELD) {
                Result<Void, RouteSeatAvailabilityError> expireResult = domain.expire();
                if (expireResult.isFailure()) {
                    return expireResult;
                }
                toSave.add(domain);
            }
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
        return Result.success();
    }

    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> cancelBookedSeats(
            RouteId routeId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> locked =
                repository.findByRouteIdAndSeatIdsForUpdate(routeId, seatIds);

        if (locked.size() != seatIds.size()) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : locked) {
            Result<Void, RouteSeatAvailabilityError> cancelResult = domain.cancel();
            if (cancelResult.isFailure()) {
                return cancelResult;
            }
            toSave.add(domain);
        }

        repository.saveAll(toSave);
        return Result.success();
    }
}
