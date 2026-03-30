package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.query.GetScheduledTripByIdQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.domain.error.ScheduledTripError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
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
    public Result<ScheduledTripDetailResponse, ScheduledTripError> execute(
            GetScheduledTripByIdQuery query) {
        return scheduledTripRepository
                .findEnrichedById(ScheduledTripId.of(query.scheduledTripId()))
                .map(scheduledTrip ->
                        Result.<ScheduledTripDetailResponse, ScheduledTripError>success(
                                toDto(scheduledTrip)))
                .orElseGet(() -> Result.failure(new ScheduledTripError.ScheduledTripNotFound()));
    }

    private ScheduledTripDetailResponse toDto(ScheduledTripEnrichedSummary scheduledTrip) {
        return new ScheduledTripDetailResponse(
                scheduledTrip.id(),
                scheduledTrip.routeTemplateId(),
                scheduledTrip.trainId(),
                scheduledTrip.departureTime(),
                scheduledTrip.arrivalTime(),
                ScheduledTripStatus.valueOf(scheduledTrip.status()),
                scheduledTrip.createdAt(),
                scheduledTrip.trainNumber() == null
                                || scheduledTrip.trainName() == null
                                || scheduledTrip.trainTotalSeats() == null
                        ? null
                        : new ScheduledTripDetailResponse.Train(
                                scheduledTrip.trainId(),
                                scheduledTrip.trainNumber(),
                                scheduledTrip.trainName(),
                                scheduledTrip.trainTotalSeats()),
                new ScheduledTripDetailResponse.Route(
                        scheduledTrip.routeTemplateId(),
                        scheduledTrip.routeBasePrice(),
                        scheduledTrip.routeCurrency(),
                        new ScheduledTripDetailResponse.Station(
                                scheduledTrip.originStationId(),
                                scheduledTrip.originStationCode(),
                                scheduledTrip.originStationName(),
                                scheduledTrip.originStationCity()),
                        new ScheduledTripDetailResponse.Station(
                                scheduledTrip.destinationStationId(),
                                scheduledTrip.destinationStationCode(),
                                scheduledTrip.destinationStationName(),
                                scheduledTrip.destinationStationCity())));
    }
}
