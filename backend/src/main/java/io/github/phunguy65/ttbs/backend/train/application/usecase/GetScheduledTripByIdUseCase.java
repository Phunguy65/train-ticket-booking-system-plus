package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetScheduledTripByIdUseCase {

    private final ScheduledTripRepository scheduledTripRepository;

    public GetScheduledTripByIdUseCase(ScheduledTripRepository scheduledTripRepository) {
        this.scheduledTripRepository = scheduledTripRepository;
    }

    @Transactional(readOnly = true)
    public Result<ScheduledTripResponse, ScheduledTripError> execute(
            GetScheduledTripByIdQuery query) {
        return scheduledTripRepository
                .findSummaryById(ScheduledTripId.of(query.scheduledTripId()))
                .map(scheduledTrip -> Result.<ScheduledTripResponse, ScheduledTripError>success(
                        toDto(scheduledTrip)))
                .orElseGet(() -> Result.failure(new ScheduledTripError.ScheduledTripNotFound()));
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
