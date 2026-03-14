package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachesQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachResponse;
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
    public PageResponse<CoachResponse> execute(GetCoachesQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("carNumber"), SortOrder.asc("id"));
        PageResponse<Coach> coaches = coachRepository.findAll(
                query.page(), query.size(), sort, TrainId.of(query.trainId()));
        return PageResponse.of(
                coaches.content().stream().map(this::toDto).toList(),
                coaches.page(),
                coaches.size(),
                coaches.hasNext(),
                coaches.total());
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
