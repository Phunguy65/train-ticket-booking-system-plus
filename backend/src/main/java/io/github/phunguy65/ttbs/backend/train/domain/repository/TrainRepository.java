package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrainRepository {

    Train save(Train train);

    boolean existsById(TrainId id);

    Optional<Train> findById(TrainId id);

    PageResponse<Train> findAll(int page, int size, List<SortOrder> sort);

    boolean existsByTrainNumber(String trainNumber);

    void softDeleteById(TrainId id, Instant deletedAt);

    int softDeleteByIds(List<TrainId> ids, Instant deletedAt);
}
