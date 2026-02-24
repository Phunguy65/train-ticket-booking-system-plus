package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.util.Optional;

public interface TrainRepository {

    Train save(Train train);

    Optional<Train> findById(TrainId id);

    PageResult<Train> findAll(int page, int size, String sortField, SortDirection direction);

    boolean existsByTrainNumber(String trainNumber);
}
