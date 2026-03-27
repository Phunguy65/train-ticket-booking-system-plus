package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.SoftDeleteTrainCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteTrainUseCase {

    private final TrainRepository trainRepository;
    private final TrainCascadeSoftDeleteService trainCascadeSoftDeleteService;

    public SoftDeleteTrainUseCase(
            TrainRepository trainRepository,
            TrainCascadeSoftDeleteService trainCascadeSoftDeleteService) {
        this.trainRepository = trainRepository;
        this.trainCascadeSoftDeleteService = trainCascadeSoftDeleteService;
    }

    @Transactional
    public Result<Void, TrainError> execute(SoftDeleteTrainCommand command) {
        if (!trainRepository.existsById(command.trainId())) {
            return Result.failure(new TrainError.TrainNotFound());
        }

        trainCascadeSoftDeleteService.execute(List.of(command.trainId()), Instant.now());

        return Result.success();
    }
}
