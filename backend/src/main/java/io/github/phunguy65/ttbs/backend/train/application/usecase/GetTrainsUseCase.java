package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetTrainsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse;
import io.github.phunguy65.ttbs.backend.train.domain.projection.TrainSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTrainsUseCase {

    private final TrainRepository trainRepository;

    public GetTrainsUseCase(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainResponse> execute(GetTrainsQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("trainNumber"), SortOrder.asc("id"));
        PageResponse<TrainSummary> trains =
                trainRepository.findAllSummaries(query.page(), query.size(), sort);
        return PageResponse.of(
                trains.content().stream().map(this::toDto).toList(),
                trains.page(),
                trains.size(),
                trains.hasNext(),
                trains.total());
    }

    private TrainResponse toDto(TrainSummary train) {
        return new TrainResponse(
                train.id(),
                train.trainNumber(),
                train.name(),
                train.totalSeats(),
                train.createdAt());
    }
}
