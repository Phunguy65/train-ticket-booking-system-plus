package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteCoachCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteCoachUseCase {

    private final CoachRepository coachRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteCoachUseCase(
            CoachRepository coachRepository, ApplicationEventPublisher eventPublisher) {
        this.coachRepository = coachRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, CoachError> execute(SoftDeleteCoachCommand command) {
        Optional<Coach> found = coachRepository.findById(command.coachId());
        if (found.isEmpty()) {
            return Result.failure(new CoachError.CoachNotFound());
        }

        Coach coach = found.get();

        if (!coach.getTrainId().equals(command.trainId())) {
            return Result.failure(new CoachError.CoachNotFound());
        }

        if (coach.isDeleted()) {
            return Result.success();
        }

        coach.softDelete();
        coachRepository.save(coach);

        for (DomainEvent event : coach.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        coach.clearDomainEvents();

        return Result.success();
    }
}
