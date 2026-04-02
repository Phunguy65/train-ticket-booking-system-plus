package io.github.phunguy65.ttbs.backend.train.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.application.port.CursorCodec;
import io.github.phunguy65.ttbs.backend.shared.domain.SliceResponse;
import io.github.phunguy65.ttbs.backend.train.application.port.ScheduledTripSearchPort;
import io.github.phunguy65.ttbs.backend.train.application.query.ScheduledTripSearchSortField;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsCursor;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsQuery;
import io.github.phunguy65.ttbs.backend.train.application.response.SearchScheduledTripsResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchScheduledTripsUseCase {

    private final ScheduledTripSearchPort scheduledTripSearchPort;
    private final CursorCodec cursorCodec;

    public SearchScheduledTripsUseCase(
            ScheduledTripSearchPort scheduledTripSearchPort, CursorCodec cursorCodec) {
        this.scheduledTripSearchPort = scheduledTripSearchPort;
        this.cursorCodec = cursorCodec;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "scheduledTripFilter",
            key = "'st-filter:' + #query.cacheKey()",
            sync = true)
    public SliceResponse<SearchScheduledTripsResponse> execute(SearchScheduledTripsQuery query) {
        SearchScheduledTripsCursor cursor = query.cursor() == null
                ? null
                : cursorCodec.decode(query.cursor(), SearchScheduledTripsCursor.class);
        SliceResponse<ScheduledTripEnrichedSummary> result =
                scheduledTripSearchPort.search(query, cursor);

        if (result.content().isEmpty()) {
            return SliceResponse.empty(query.size());
        }

        return SliceResponse.of(
                result.content().stream().map(this::toDto).toList(),
                result.size(),
                result.hasNext(),
                result.hasNext()
                        ? cursorCodec.encode(nextCursor(
                                query.sortBy(),
                                result.content().get(result.content().size() - 1)))
                        : null);
    }

    private SearchScheduledTripsResponse toDto(ScheduledTripEnrichedSummary scheduledTrip) {
        return new SearchScheduledTripsResponse(
                scheduledTrip.id(),
                scheduledTrip.departureTime(),
                scheduledTrip.arrivalTime(),
                ScheduledTripStatus.valueOf(scheduledTrip.status()),
                scheduledTrip.durationMinutes(),
                scheduledTrip.availableSeatCount(),
                occupancyPercentage(scheduledTrip),
                scheduledTrip.trainId() == null
                                || scheduledTrip.trainNumber() == null
                                || scheduledTrip.trainName() == null
                                || scheduledTrip.trainTotalSeats() == null
                        ? null
                        : new SearchScheduledTripsResponse.Train(
                                scheduledTrip.trainId(),
                                scheduledTrip.trainNumber(),
                                scheduledTrip.trainName(),
                                scheduledTrip.trainTotalSeats()),
                new SearchScheduledTripsResponse.Route(
                        scheduledTrip.routeTemplateId(),
                        scheduledTrip.routeBasePrice(),
                        scheduledTrip.routeCurrency(),
                        new SearchScheduledTripsResponse.Station(
                                scheduledTrip.originStationId(),
                                scheduledTrip.originStationCode(),
                                scheduledTrip.originStationName(),
                                scheduledTrip.originStationCity()),
                        new SearchScheduledTripsResponse.Station(
                                scheduledTrip.destinationStationId(),
                                scheduledTrip.destinationStationCode(),
                                scheduledTrip.destinationStationName(),
                                scheduledTrip.destinationStationCity())));
    }

    private SearchScheduledTripsCursor nextCursor(
            ScheduledTripSearchSortField sortField, ScheduledTripEnrichedSummary scheduledTrip) {
        return new SearchScheduledTripsCursor(
                sortValue(sortField, scheduledTrip), scheduledTrip.id());
    }

    private String sortValue(
            ScheduledTripSearchSortField sortField, ScheduledTripEnrichedSummary scheduledTrip) {
        return switch (sortField) {
            case DEPARTURE_TIME -> scheduledTrip.departureTime().toString();
            case PRICE -> Long.toString(scheduledTrip.routeBasePrice());
            case DURATION -> Long.toString(scheduledTrip.durationMinutes());
            case AVAILABLE_SEATS -> Long.toString(scheduledTrip.availableSeatCount());
        };
    }

    private int occupancyPercentage(ScheduledTripEnrichedSummary scheduledTrip) {
        Integer totalSeats = scheduledTrip.trainTotalSeats();
        if (totalSeats == null || totalSeats <= 0) {
            return 0;
        }

        long bookedSeats =
                Math.max(0L, totalSeats.longValue() - scheduledTrip.availableSeatCount());
        long percentage = Math.round((bookedSeats * 100.0d) / totalSeats);
        return (int) Math.max(0L, Math.min(100L, percentage));
    }
}
