package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteCoachesCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.event.CoachesDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteCoachesUseCase {

    private final CoachRepository coachRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteCoachesUseCase(
            CoachRepository coachRepository, ApplicationEventPublisher eventPublisher) {
        this.coachRepository = coachRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, CoachError> execute(BulkSoftDeleteCoachesCommand command) {
        Instant now = Instant.now();
        int affected = coachRepository.softDeleteByIds(command.coachIds(), now);

        if (affected > 0) {
            eventPublisher.publishEvent(CoachesDeleted.of(command.coachIds(), now));
        }

        return Result.success(affected);
    }
}
