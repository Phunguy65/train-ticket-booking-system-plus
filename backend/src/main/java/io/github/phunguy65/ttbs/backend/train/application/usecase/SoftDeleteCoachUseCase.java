package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteCoachCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteCoachUseCase {

    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteCoachUseCase(
            CoachRepository coachRepository,
            SeatRepository seatRepository,
            ApplicationEventPublisher eventPublisher) {
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
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

        if (!seatRepository.findByCoachId(command.coachId()).isEmpty()) {
            return Result.failure(
                    new CoachError.CoachInUse(List.of(command.coachId().value())));
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
