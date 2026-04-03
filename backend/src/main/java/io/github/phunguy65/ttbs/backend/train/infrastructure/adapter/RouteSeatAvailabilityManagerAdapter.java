package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements {@link RouteSeatAvailabilityManager} using the domain
 * repository and optimistic locking ({@code @Version}) for concurrent modification detection.
 *
 * <p>Placed in {@code train.infrastructure.adapter} — the port contract lives in
 * {@code train.domain.model} and is exposed via the {@code train::model} named interface.
 */
@Service
public class RouteSeatAvailabilityManagerAdapter implements RouteSeatAvailabilityManager {

    private final RouteSeatAvailabilityRepository repository;

    public RouteSeatAvailabilityManagerAdapter(RouteSeatAvailabilityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> holdSeats(
            ScheduledTripId scheduledTripId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> seats =
                repository.findByScheduledTripIdAndSeatIds(scheduledTripId, seatIds);

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
            ScheduledTripId scheduledTripId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> seats =
                repository.findByScheduledTripIdAndSeatIds(scheduledTripId, seatIds);

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
            ScheduledTripId scheduledTripId, List<SeatId> seatIds) {
        List<RouteSeatAvailability> seats =
                repository.findByScheduledTripIdAndSeatIds(scheduledTripId, seatIds);

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
    public List<RouteSeatAvailability> findByBookingId(java.util.UUID bookingId) {
        return repository.findByBookingId(bookingId);
    }

    @Override
    public List<RouteSeatAvailability> findByScheduledTripIdAndSeatIds(
            ScheduledTripId scheduledTripId, List<SeatId> seatIds) {
        return repository.findByScheduledTripIdAndSeatIds(scheduledTripId, seatIds);
    }

    @Override
    public List<RouteSeatAvailability> findAllByScheduledTripId(ScheduledTripId scheduledTripId) {
        return repository.findAllByScheduledTripId(scheduledTripId);
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

    /**
     * Atomically transitions all specified seats from {@code AVAILABLE} to {@code HELD} with
     * a specific booking ID and price snapshot.
     *
     * @param scheduledTripId the scheduled trip
     * @param seatIds the seats to hold
     * @param bookingId the booking ID to associate with the held seats
     * @param price the price snapshot captured at booking time
     * @return success if all seats were AVAILABLE and are now HELD with price captured;
     * failure with {@link RouteSeatAvailabilityError.SeatNotAvailable} otherwise
     */
    @Override
    @Transactional
    public Result<Void, RouteSeatAvailabilityError> holdSeatsWithBookingId(
            ScheduledTripId scheduledTripId,
            List<SeatId> seatIds,
            java.util.UUID bookingId,
            Money price) {
        List<RouteSeatAvailability> seats =
                repository.findByScheduledTripIdAndSeatIds(scheduledTripId, seatIds);

        if (seats.size() != seatIds.size()) {
            return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
        }

        List<RouteSeatAvailability> toSave = new ArrayList<>();
        for (RouteSeatAvailability domain : seats) {
            Result<Void, RouteSeatAvailabilityError> holdResult = domain.hold(bookingId, price);
            if (holdResult.isFailure()) {
                return holdResult;
            }
            toSave.add(domain);
        }

        repository.saveAll(toSave);
        return Result.success();
    }
}
