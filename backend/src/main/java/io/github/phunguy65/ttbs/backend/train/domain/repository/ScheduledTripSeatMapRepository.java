package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapCoachSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.CoachSeatMapSeatSummary;
import java.util.List;

public interface ScheduledTripSeatMapRepository {

    PageResponse<CoachSeatMapCoachSummary> findCoachSummariesByScheduledTripId(
            int page, int size, ScheduledTripId scheduledTripId);

    List<CoachSeatMapSeatSummary> findSeatSummariesByScheduledTripIdAndCoachIds(
            ScheduledTripId scheduledTripId, List<CoachId> coachIds);
}
