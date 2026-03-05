package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.dto.TrainDto;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetTrainByIdUseCase {

    private final TrainRepository trainRepository;

    public GetTrainByIdUseCase(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Transactional(readOnly = true)
    public Result<TrainDto, TrainError> execute(TrainId trainId) {
        return trainRepository
                .findById(trainId)
                .map(train -> Result.<TrainDto, TrainError>success(toDto(train)))
                .orElseGet(() -> Result.failure(new TrainError.TrainNotFound()));
    }

    private TrainDto toDto(Train train) {
        return new TrainDto(
                train.getId().value(),
                train.getTrainNumber(),
                train.getName(),
                train.getTotalSeats(),
                train.getCreatedAt());
    }
}
