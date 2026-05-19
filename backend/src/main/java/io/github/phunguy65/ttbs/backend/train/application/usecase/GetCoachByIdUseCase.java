package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.CoachError;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSummary;
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
    public Result<CoachResponse, CoachError> execute(GetCoachByIdQuery query) {
        CoachId coachId = CoachId.of(query.coachId());
        TrainId trainId = TrainId.of(query.trainId());
        return coachRepository
                .findSummaryById(coachId)
                .filter(coach -> coach.trainId().equals(trainId.value()))
                .map(coach -> Result.<CoachResponse, CoachError>success(toDto(coach)))
                .orElseGet(() -> Result.failure(new CoachError.CoachNotFound()));
    }

    private CoachResponse toDto(CoachSummary coach) {
        return new CoachResponse(
                coach.id(),
                coach.trainId(),
                coach.carNumber(),
                coach.totalSeats(),
                coach.createdAt());
    }
}
