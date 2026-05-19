package io.github.phunguy65.ttbs.backend.train.application.query;

import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import java.time.LocalDate;
import java.util.UUID;

public record SearchScheduledTripsQuery(
        UUID originStationId,
        UUID destinationStationId,
        LocalDate departureDate,
        String status,
        boolean availableOnly,
        Long minPrice,
        Long maxPrice,
        ScheduledTripSearchSortField sortBy,
        SortOrder.Direction sortDirection,
        String cursor,
        int size) {

    public SearchScheduledTripsQuery {
        status = normalize(status);
        cursor = normalize(cursor);
    }

    public String cacheKey() {
        return String.join(
                ":",
                value(originStationId),
                value(destinationStationId),
                value(departureDate),
                value(status),
                Boolean.toString(availableOnly),
                value(minPrice),
                value(maxPrice),
                sortBy.name(),
                sortDirection.name(),
                Integer.toString(size));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String value(Object value) {
        return value == null ? "_" : value.toString();
    }
}
