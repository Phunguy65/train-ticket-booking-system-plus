package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.command.BulkSoftDeleteTrainsCommand;
import io.github.phunguy65.ttbs.backend.train.domain.error.TrainError;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteTrainsUseCase {

    private final TrainCascadeSoftDeleteService trainCascadeSoftDeleteService;

    public BulkSoftDeleteTrainsUseCase(
            TrainCascadeSoftDeleteService trainCascadeSoftDeleteService) {
        this.trainCascadeSoftDeleteService = trainCascadeSoftDeleteService;
    }

    @Transactional
    public Result<Integer, TrainError> execute(BulkSoftDeleteTrainsCommand command) {
        int affected = trainCascadeSoftDeleteService.execute(command.trainIds(), Instant.now());

        return Result.success(affected);
    }
}
