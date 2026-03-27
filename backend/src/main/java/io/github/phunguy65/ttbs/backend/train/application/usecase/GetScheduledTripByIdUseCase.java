package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTrip;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
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
            ScheduledTripId scheduledTripId) {
        return scheduledTripRepository
                .findById(scheduledTripId)
                .map(scheduledTrip -> Result.<ScheduledTripResponse, ScheduledTripError>success(
                        toDto(scheduledTrip)))
                .orElseGet(() -> Result.failure(new ScheduledTripError.ScheduledTripNotFound()));
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
