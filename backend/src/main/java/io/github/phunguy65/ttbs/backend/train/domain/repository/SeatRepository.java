package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.SeatSummary;
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

    List<Seat> saveAll(List<Seat> seats);

    List<Seat> findByCoachId(CoachId coachId);

    PageResponse<Seat> findAll(int page, int size, List<SortOrder> sort, TrainId trainId);

    PageResponse<SeatSummary> findAllSummaries(
            int page, int size, List<SortOrder> sort, TrainId trainId);

    PageResponse<Seat> findAllAvailable(
            int page, int size, List<SortOrder> sort, ScheduledTripId scheduledTripId);

    PageResponse<SeatSummary> findAllAvailableSummaries(
            int page, int size, List<SortOrder> sort, ScheduledTripId scheduledTripId);

    List<SeatId> findActiveIdsByCoachIds(List<CoachId> coachIds);

    Optional<Seat> findById(SeatId id);

    boolean existsByCoachIdAndSeatNumber(CoachId coachId, String seatNumber);

    int countActiveByTrainId(TrainId trainId);

    int countActiveByCoachId(CoachId coachId);

    List<TrainId> findDistinctTrainIdsBySeatIds(List<SeatId> seatIds);

    List<CoachId> findDistinctCoachIdsBySeatIds(List<SeatId> seatIds);

    void softDeleteById(SeatId id, Instant deletedAt);

    int softDeleteByIds(List<SeatId> ids, Instant deletedAt);
}
