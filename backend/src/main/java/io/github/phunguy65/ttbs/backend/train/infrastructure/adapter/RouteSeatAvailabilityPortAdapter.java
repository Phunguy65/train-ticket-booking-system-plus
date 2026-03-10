package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements {@link RouteSeatAvailabilityPort} using the domain
 * repository and optimistic locking ({@code @Version}) for concurrent modification detection.
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
    public Result<Void, RouteSeatAvailabilityError> holdSeats(
            RouteId routeId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> seats = repository.findByRouteIdAndSeatIds(routeId, seatIds);

        if (seats.size() != seatIds.size()) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : seats) {
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
    public Result<Void, RouteSeatAvailabilityError> releaseHeldSeats(
            RouteId routeId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> seats = repository.findByRouteIdAndSeatIds(routeId, seatIds);

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : seats) {
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
        List<RouteSeatAvailability> seats = repository.findByRouteIdAndSeatIds(routeId, seatIds);

        if (seats.size() != seatIds.size()) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : seats) {
            Result<Void, RouteSeatAvailabilityError> cancelResult = domain.cancel();
            if (cancelResult.isFailure()) {
                return cancelResult;
            }
            toSave.add(domain);
        }

        repository.saveAll(toSave);
        return Result.success();
    }

    @Override
    public List<SeatId> findSeatIdsByBookingId(java.util.UUID bookingId) {
        return repository.findByBookingId(bookingId).stream()
                .map(RouteSeatAvailability::getSeatId)
                .toList();
    }

    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> confirmHeldSeats(java.util.UUID bookingId) {
        List<RouteSeatAvailability> seats = repository.findByBookingId(bookingId);

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : seats) {
            if (domain.getStatus() == RouteSeatAvailabilityStatus.HELD) {
                Result<Void, RouteSeatAvailabilityError> result = domain.confirmHold();
                if (result.isFailure()) {
                    return result;
                }
                toSave.add(domain);
            }
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
        return Result.success();
    }
}
