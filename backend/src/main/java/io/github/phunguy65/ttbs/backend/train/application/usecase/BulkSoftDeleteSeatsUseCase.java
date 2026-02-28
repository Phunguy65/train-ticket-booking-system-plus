package io.github.phunguy65.ttbs.backend.train.application.usecase;

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
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteSeatsUseCase(
            SeatRepository seatRepository,
            RouteSeatAvailabilityRepository availabilityRepository,
            ApplicationEventPublisher eventPublisher) {
        this.seatRepository = seatRepository;
        this.availabilityRepository = availabilityRepository;
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

        Instant now = Instant.now();
        int affected = seatRepository.softDeleteByIds(command.seatIds(), now);

        for (SeatId seatId : command.seatIds()) {
            eventPublisher.publishEvent(SeatDeleted.of(seatId));
        }

        return Result.success(affected);
    }
}
