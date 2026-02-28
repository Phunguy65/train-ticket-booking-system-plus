package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteTrainCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteTrainUseCase {

    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteTrainUseCase(
            TrainRepository trainRepository,
            RouteRepository routeRepository,
            ApplicationEventPublisher eventPublisher) {
        this.trainRepository = trainRepository;
        this.routeRepository = routeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, TrainError> execute(SoftDeleteTrainCommand command) {
        Optional<Train> found = trainRepository.findById(command.trainId());
        if (found.isEmpty()) {
            return Result.failure(new TrainError.TrainNotFound());
        }

        Train train = found.get();

        if (train.isDeleted()) {
            return Result.success();
        }

        if (routeRepository.existsActiveByTrainId(command.trainId())) {
            return Result.failure(new TrainError.TrainInUse(
                    java.util.List.of(command.trainId().value())));
        }

        train.softDelete();
        trainRepository.save(train);

        for (DomainEvent event : train.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        train.clearDomainEvents();

        return Result.success();
    }
}
