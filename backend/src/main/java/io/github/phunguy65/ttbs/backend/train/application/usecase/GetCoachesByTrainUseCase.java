package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.train.application.dto.CoachDto;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.CoachRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCoachesByTrainUseCase {

    private final CoachRepository coachRepository;

    public GetCoachesByTrainUseCase(CoachRepository coachRepository) {
        this.coachRepository = coachRepository;
    }

    @Transactional(readOnly = true)
    public List<CoachDto> execute(TrainId trainId) {
        return coachRepository.findByTrainId(trainId).stream().map(this::toDto).toList();
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
