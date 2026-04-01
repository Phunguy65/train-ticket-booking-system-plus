package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetCoachSeatMapQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.CoachSeatMapResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapCoachSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapSeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripSeatMapRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCoachSeatMapByScheduledTripUseCase {

    private final ScheduledTripRepository scheduledTripRepository;
    private final ScheduledTripSeatMapRepository scheduledTripSeatMapRepository;

    public GetCoachSeatMapByScheduledTripUseCase(
            ScheduledTripRepository scheduledTripRepository,
            ScheduledTripSeatMapRepository scheduledTripSeatMapRepository) {
        this.scheduledTripRepository = scheduledTripRepository;
        this.scheduledTripSeatMapRepository = scheduledTripSeatMapRepository;
    }

    @Transactional(readOnly = true)
    public Result<PageResponse<CoachSeatMapResponse>, ScheduledTripError> execute(
            GetCoachSeatMapQuery query) {
        ScheduledTripId scheduledTripId = ScheduledTripId.of(query.scheduledTripId());
        PageResponse<CoachSeatMapCoachSummary> coaches =
                scheduledTripSeatMapRepository.findCoachSummariesByScheduledTripId(
                        query.page(), query.size(), scheduledTripId);

        if (coaches.content().isEmpty()) {
            if (!scheduledTripRepository.existsById(scheduledTripId)) {
                return Result.failure(new ScheduledTripError.ScheduledTripNotFound());
            }
            return Result.success(PageResponse.of(
                    List.of(), coaches.page(), coaches.size(), coaches.hasNext(), coaches.total()));
        }

        List<CoachId> coachIds =
                coaches.content().stream().map(c -> CoachId.of(c.id())).toList();
        Map<UUID, List<CoachSeatMapResponse.Seat>> seatsByCoachId = groupSeatsByCoachId(
                scheduledTripSeatMapRepository.findSeatSummariesByScheduledTripIdAndCoachIds(
                        scheduledTripId, coachIds));

        List<CoachSeatMapResponse> content = coaches.content().stream()
                .map(coach -> new CoachSeatMapResponse(
                        coach.id(),
                        coach.carNumber(),
                        coach.totalSeats(),
                        seatsByCoachId.getOrDefault(coach.id(), List.of())))
                .toList();

        return Result.success(PageResponse.of(
                content, coaches.page(), coaches.size(), coaches.hasNext(), coaches.total()));
    }

    private Map<UUID, List<CoachSeatMapResponse.Seat>> groupSeatsByCoachId(
            List<CoachSeatMapSeatSummary> seatSummaries) {
        Map<UUID, List<CoachSeatMapResponse.Seat>> seatsByCoachId = new LinkedHashMap<>();
        for (CoachSeatMapSeatSummary seat : seatSummaries) {
            seatsByCoachId
                    .computeIfAbsent(seat.coachId(), ignored -> new java.util.ArrayList<>())
                    .add(new CoachSeatMapResponse.Seat(
                            seat.id(), seat.seatNumber(), seat.status()));
        }
        return seatsByCoachId;
    }
}
