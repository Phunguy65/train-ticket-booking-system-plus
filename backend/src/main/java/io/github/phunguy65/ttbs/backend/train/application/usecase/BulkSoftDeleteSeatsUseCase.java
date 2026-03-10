package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteSeatsCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.SeatError;
import io.github.phunguy65.ttbs.backend.train.domain.event.SeatsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteSeatsUseCase {

    private final SeatRepository seatRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteSeatsUseCase(
            SeatRepository seatRepository, ApplicationEventPublisher eventPublisher) {
        this.seatRepository = seatRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, SeatError> execute(BulkSoftDeleteSeatsCommand command) {
        Instant now = Instant.now();
        int affected = seatRepository.softDeleteByIds(command.seatIds(), now);

        if (affected > 0) {
            eventPublisher.publishEvent(SeatsDeleted.of(command.seatIds(), now));
        }

        return Result.success(affected);
    }
}
