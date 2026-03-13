package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.UpdateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateTrainUseCase {

    private final TrainRepository trainRepository;

    public UpdateTrainUseCase(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Transactional
    public Result<TrainResponse, TrainError> execute(UpdateTrainCommand command) {
        Train train = trainRepository.findById(command.trainId()).orElse(null);
        if (train == null) {
            return Result.failure(new TrainError.TrainNotFound());
        }

        JsonNullable<String> trainNumberField = command.trainNumber();
        if (trainNumberField.isPresent()) {
            String newTrainNumber = trainNumberField.get();
            if (newTrainNumber != null && !newTrainNumber.equals(train.getTrainNumber())) {
                if (trainRepository.existsByTrainNumber(newTrainNumber)) {
                    return Result.failure(new TrainError.TrainNumberAlreadyExists(newTrainNumber));
                }
            }
        }

        String newTrainNumber =
                trainNumberField.isPresent() ? trainNumberField.get() : train.getTrainNumber();
        String newName = command.name().isPresent() ? command.name().get() : train.getName();
        int newTotalSeats = command.totalSeats().isPresent()
                ? command.totalSeats().get()
                : train.getTotalSeats();

        Train updated = Train.reconstitute(
                train.getId(),
                newTrainNumber,
                newName,
                newTotalSeats,
                train.getCreatedAt(),
                train.getDeletedAt());

        Train saved = trainRepository.save(updated);
        return Result.success(toDto(saved));
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
