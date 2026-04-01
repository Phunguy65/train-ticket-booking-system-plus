package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.cache.ValkeyCacheConfig;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetScheduledTripsUseCase {

    private final ScheduledTripRepository scheduledTripRepository;

    public GetScheduledTripsUseCase(ScheduledTripRepository scheduledTripRepository) {
        this.scheduledTripRepository = scheduledTripRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = ValkeyCacheConfig.SCHEDULED_TRIP_LIST_CACHE,
            key = "'st-list:' + #query.page() + ':' + #query.size()",
            sync = true)
    public PageResponse<ScheduledTripResponse> execute(GetScheduledTripsQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"));
        PageResponse<ScheduledTripSummary> scheduledTrips =
                scheduledTripRepository.findAllSummaries(query.page(), query.size(), sort);
        return PageResponse.of(
                scheduledTrips.content().stream().map(this::toDto).toList(),
                scheduledTrips.page(),
                scheduledTrips.size(),
                scheduledTrips.hasNext(),
                scheduledTrips.total());
    }

    private ScheduledTripResponse toDto(ScheduledTripSummary scheduledTrip) {
        return new ScheduledTripResponse(
                scheduledTrip.id(),
                scheduledTrip.routeTemplateId(),
                scheduledTrip.trainId(),
                scheduledTrip.departureTime(),
                scheduledTrip.arrivalTime(),
                ScheduledTripStatus.valueOf(scheduledTrip.status()),
                scheduledTrip.createdAt());
    }
}
