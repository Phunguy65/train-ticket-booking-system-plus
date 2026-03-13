package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTrainsUseCase {

    private final TrainRepository trainRepository;

    public GetTrainsUseCase(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<TrainResponse> execute(
            int page, int size, String sortField, SortDirection direction) {
        PageResult<Train> trains = trainRepository.findAll(page, size, sortField, direction);
        return PageResult.of(
                trains.items().stream().map(this::toDto).toList(),
                trains.pageNumber(),
                trains.pageSize(),
                trains.hasNext());
    }

    private TrainResponse toDto(Train train) {
        return new TrainResponse(
                train.getId().value(),
                train.getTrainNumber(),
                train.getName(),
                train.getTotalSeats(),
                train.getCreatedAt());
    }
}
