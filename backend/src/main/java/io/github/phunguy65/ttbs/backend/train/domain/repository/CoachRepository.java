package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence contract for the {@link Coach} aggregate.
 *
 * <p>No JPA or Spring framework types appear here.
 */
public interface CoachRepository {

    Coach save(Coach coach);

    Optional<Coach> findById(CoachId id);

    List<Coach> findByTrainId(TrainId trainId);

    boolean existsByTrainIdAndCarNumber(TrainId trainId, int carNumber);

    void softDeleteById(CoachId id, Instant deletedAt);

    int softDeleteByIds(List<CoachId> ids, Instant deletedAt);
}
