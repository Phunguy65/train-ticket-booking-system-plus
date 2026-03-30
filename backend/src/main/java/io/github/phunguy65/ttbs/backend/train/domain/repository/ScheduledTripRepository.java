package io.github.phunguy65.ttbs.backend.train.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteTemplateId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTrip;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ScheduledTripRepository {

    ScheduledTrip save(ScheduledTrip scheduledTrip);

    Optional<ScheduledTrip> findById(ScheduledTripId id);

    Optional<ScheduledTripSummary> findSummaryById(ScheduledTripId id);

    Optional<ScheduledTripEnrichedSummary> findEnrichedById(ScheduledTripId id);

    PageResponse<ScheduledTrip> findAll(int page, int size, List<SortOrder> sort);

    PageResponse<ScheduledTripSummary> findAllSummaries(int page, int size, List<SortOrder> sort);

    PageResponse<ScheduledTripEnrichedSummary> findAllEnrichedSummaries(int page, int size);

    boolean existsById(ScheduledTripId id);

    List<ScheduledTripId> findActiveIdsByTrainIds(List<TrainId> trainIds);

    List<ScheduledTripId> findActiveIdsByRouteTemplateId(RouteTemplateId routeTemplateId);

    void softDeleteById(ScheduledTripId id, Instant deletedAt);

    int softDeleteByIds(List<ScheduledTripId> ids, Instant deletedAt);
}
