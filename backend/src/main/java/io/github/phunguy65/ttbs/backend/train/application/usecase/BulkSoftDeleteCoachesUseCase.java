package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteCoachesCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.event.CoachDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteCoachesUseCase {

    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteCoachesUseCase(
            CoachRepository coachRepository,
            SeatRepository seatRepository,
            ApplicationEventPublisher eventPublisher) {
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, CoachError> execute(BulkSoftDeleteCoachesCommand command) {
        List<UUID> conflictingIds = command.coachIds().stream()
                .filter(coachId -> !seatRepository.findByCoachId(coachId).isEmpty())
                .map(CoachId::value)
                .toList();

        if (!conflictingIds.isEmpty()) {
            return Result.failure(new CoachError.CoachInUse(conflictingIds));
        }

        Instant now = Instant.now();
        int affected = coachRepository.softDeleteByIds(command.coachIds(), now);

        for (CoachId coachId : command.coachIds()) {
            eventPublisher.publishEvent(CoachDeleted.of(coachId));
        }

        return Result.success(affected);
    }
}
