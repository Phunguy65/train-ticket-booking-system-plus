package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteTrainsCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.event.TrainsDeleted;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteTrainsUseCase {

    private final TrainRepository trainRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteTrainsUseCase(
            TrainRepository trainRepository, ApplicationEventPublisher eventPublisher) {
        this.trainRepository = trainRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Integer, TrainError> execute(BulkSoftDeleteTrainsCommand command) {
        Instant now = Instant.now();
        int affected = trainRepository.softDeleteByIds(command.trainIds(), now);

        if (affected > 0) {
            eventPublisher.publishEvent(TrainsDeleted.of(command.trainIds(), now));
        }

        return Result.success(affected);
    }
}
