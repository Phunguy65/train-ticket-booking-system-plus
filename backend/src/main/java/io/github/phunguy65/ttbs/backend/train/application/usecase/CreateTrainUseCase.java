package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.train.application.command.CreateTrainCommand;
import io.github.phunguy65.ttbs.backend.train.application.response.TrainResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateTrainUseCase {

    private final TrainRepository trainRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CreateTrainUseCase(
            TrainRepository trainRepository, ApplicationEventPublisher eventPublisher) {
        this.trainRepository = trainRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<TrainResponse, TrainError> execute(CreateTrainCommand command) {
        if (trainRepository.existsByTrainNumber(command.trainNumber())) {
            return Result.failure(new TrainError.TrainNumberAlreadyExists(command.trainNumber()));
        }

        TrainId trainId = TrainId.of(UuidGenerator.generate());
        Train train =
                Train.create(trainId, command.trainNumber(), command.name(), command.totalSeats());
        Train saved = trainRepository.save(train);

        for (DomainEvent event : train.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        train.clearDomainEvents();

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
