package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.dto.CoachDto;
import io.github.phunguy65.ttbs.backend.train.domain.errors.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCoachByIdUseCase {

    private final CoachRepository coachRepository;

    public GetCoachByIdUseCase(CoachRepository coachRepository) {
        this.coachRepository = coachRepository;
    }

    @Transactional(readOnly = true)
    public Result<CoachDto, CoachError> execute(CoachId coachId, TrainId trainId) {
        return coachRepository
                .findById(coachId)
                .filter(coach -> coach.getTrainId().equals(trainId))
                .map(coach -> Result.<CoachDto, CoachError>success(toDto(coach)))
                .orElseGet(() -> Result.failure(new CoachError.CoachNotFound()));
    }

    private CoachDto toDto(Coach coach) {
        return new CoachDto(
                coach.getId().value(),
                coach.getTrainId().value(),
                coach.getCarNumber(),
                coach.getTotalSeats(),
                coach.getCreatedAt());
    }
}
