package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteTrainsCommand;
import io.github.phunguy65.ttbs.backend.train.domain.errors.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteTrainsUseCase {

    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteTrainsUseCase(
            TrainRepository trainRepository,
            RouteRepository routeRepository,
            ApplicationEventPublisher eventPublisher) {
        this.trainRepository = trainRepository;
        this.routeRepository = routeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, TrainError> execute(BulkSoftDeleteTrainsCommand command) {
        List<UUID> conflictingIds = command.trainIds().stream()
                .filter(trainId -> routeRepository.existsActiveByTrainId(trainId))
                .map(TrainId::value)
                .toList();

        if (!conflictingIds.isEmpty()) {
            return Result.failure(new TrainError.TrainInUse(conflictingIds));
        }

        Instant now = Instant.now();
        int affected = trainRepository.softDeleteByIds(command.trainIds(), now);

        for (TrainId trainId : command.trainIds()) {
            eventPublisher.publishEvent(TrainDeleted.of(trainId));
        }

        return Result.success(affected);
    }
}
