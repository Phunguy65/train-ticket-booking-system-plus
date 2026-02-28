package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Domain-facing persistence contract for the {@link Seat} aggregate.
 *
 * <p>No JPA or Spring framework types appear here.
 */
public interface SeatRepository {

    Seat save(Seat seat);

    List<Seat> findByTrainId(TrainId trainId);

    Optional<Seat> findById(SeatId id);

    boolean existsByTrainIdAndSeatNumber(TrainId trainId, String seatNumber);

    void softDeleteById(SeatId id, Instant deletedAt);

    int softDeleteByIds(List<SeatId> ids, Instant deletedAt);
}
