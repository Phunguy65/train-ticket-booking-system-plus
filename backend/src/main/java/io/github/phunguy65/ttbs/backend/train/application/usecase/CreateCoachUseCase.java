package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateCoachCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCoachUseCase {

    private final TrainRepository trainRepository;
    private final CoachRepository coachRepository;

    public CreateCoachUseCase(TrainRepository trainRepository, CoachRepository coachRepository) {
        this.trainRepository = trainRepository;
        this.coachRepository = coachRepository;
    }

    @Transactional
    public Result<CoachResponse, CoachError> execute(CreateCoachCommand command) {
        TrainId trainId = TrainId.of(command.trainId());

        if (trainRepository.findById(trainId).isEmpty()) {
            return Result.failure(new CoachError.TrainNotFound());
        }

        if (coachRepository.existsByTrainIdAndCarNumber(trainId, command.carNumber())) {
            return Result.failure(new CoachError.CarNumberAlreadyExists(command.carNumber()));
        }

        Coach coach =
                Coach.create(CoachId.of(UuidGenerator.generate()), trainId, command.carNumber());
        Coach saved = coachRepository.save(coach);

        return Result.success(toDto(saved));
    }

    private CoachResponse toDto(Coach coach) {
        return new CoachResponse(
                coach.getId().value(),
                coach.getTrainId().value(),
                coach.getCarNumber(),
                coach.getTotalSeats(),
                coach.getCreatedAt());
    }
}
