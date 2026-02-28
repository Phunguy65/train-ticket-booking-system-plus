package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.port.SeatValidationPort;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteSeatsCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteSeatsUseCase {

    private final SeatRepository seatRepository;
    private final RouteSeatAvailabilityRepository availabilityRepository;
    private final SeatValidationPort seatValidationPort;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteSeatsUseCase(
            SeatRepository seatRepository,
            RouteSeatAvailabilityRepository availabilityRepository,
            SeatValidationPort seatValidationPort,
            ApplicationEventPublisher eventPublisher) {
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
        this.seatValidationPort = seatValidationPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, SeatError> execute(BulkSoftDeleteSeatsCommand command) {
        List<UUID> conflictingIds = command.seatIds().stream()
                .filter(seatId -> availabilityRepository.existsActiveBySeatId(seatId))
                .map(SeatId::value)
                .toList();

        if (!conflictingIds.isEmpty()) {
            return Result.failure(new SeatError.SeatInUse(conflictingIds));
        }

        List<UUID> bookingConflictIds = command.seatIds().stream()
                .filter(seatValidationPort::hasBookingHistoryForSeat)
                .map(SeatId::value)
                .toList();

        if (!bookingConflictIds.isEmpty()) {
            return Result.failure(new SeatError.SeatHasBookingHistory(bookingConflictIds));
        }

        Instant now = Instant.now();
        int affected = seatRepository.softDeleteByIds(command.seatIds(), now);

        for (SeatId seatId : command.seatIds()) {
            eventPublisher.publishEvent(SeatDeleted.of(seatId));
        }

        return Result.success(affected);
    }
}
