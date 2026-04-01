package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapCoachSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapSeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripSeatMapRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class ScheduledTripSeatMapRepositoryAdapter implements ScheduledTripSeatMapRepository {

    private final RouteSeatAvailabilityJpaRepository jpaRepository;

    ScheduledTripSeatMapRepositoryAdapter(RouteSeatAvailabilityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PageResponse<CoachSeatMapCoachSummary> findCoachSummariesByScheduledTripId(
            int page, int size, ScheduledTripId scheduledTripId) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<CoachSeatMapCoachSummaryView> result =
                jpaRepository.findCoachSummariesByScheduledTripId(
                        scheduledTripId.value(), pageable);
        List<CoachSeatMapCoachSummary> items = result.getContent().stream()
                .map(view -> new CoachSeatMapCoachSummary(
                        view.getId(), view.getCarNumber(), view.getTotalSeats()))
                .toList();
        return PageResponse.of(items, page, size, result.hasNext(), result.getTotalElements());
    }

    @Override
    public List<CoachSeatMapSeatSummary> findSeatSummariesByScheduledTripIdAndCoachIds(
            ScheduledTripId scheduledTripId, List<CoachId> coachIds) {
        List<UUID> uuids = coachIds.stream().map(CoachId::value).toList();
        return jpaRepository
                .findSeatSummariesByScheduledTripIdAndCoachIds(scheduledTripId.value(), uuids)
                .stream()
                .map(view -> new CoachSeatMapSeatSummary(
                        view.getId(),
                        view.getCoachId(),
                        view.getSeatNumber(),
                        RouteSeatAvailabilityStatus.valueOf(view.getStatus())))
                .toList();
    }
}
