package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTrip;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetScheduledTripsUseCase {

    private final ScheduledTripRepository scheduledTripRepository;

    public GetScheduledTripsUseCase(ScheduledTripRepository scheduledTripRepository) {
        this.scheduledTripRepository = scheduledTripRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduledTripResponse> execute(GetScheduledTripsQuery query) {
        List<SortOrder> sort = List.of(SortOrder.asc("departureTime"), SortOrder.asc("id"));
        PageResponse<ScheduledTrip> scheduledTrips =
                scheduledTripRepository.findAll(query.page(), query.size(), sort);
        return PageResponse.of(
                scheduledTrips.content().stream().map(this::toDto).toList(),
                scheduledTrips.page(),
                scheduledTrips.size(),
                scheduledTrips.hasNext(),
                scheduledTrips.total());
    }

    private ScheduledTripResponse toDto(ScheduledTrip scheduledTrip) {
        return new ScheduledTripResponse(
                scheduledTrip.getId().value(),
                scheduledTrip.getRouteTemplateId().value(),
                scheduledTrip.getTrainId() == null
                        ? null
                        : scheduledTrip.getTrainId().value(),
                scheduledTrip.getDepartureTime(),
                scheduledTrip.getArrivalTime(),
                scheduledTrip.getStatus(),
                scheduledTrip.getCreatedAt());
    }
}
