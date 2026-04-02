package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.ScheduledTripSearchSortField;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record SearchScheduledTripsRequest(
        UUID originStationId,
        UUID destinationStationId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,

        @Pattern(
                regexp = "SCHEDULED|BOARDING|DEPARTED|ARRIVED|CANCELLED",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of SCHEDULED, BOARDING, DEPARTED, ARRIVED, CANCELLED")
        String status,

        Boolean availableOnly,
        @Min(0) Long minPrice,
        @Min(0) Long maxPrice,

        @Pattern(
                regexp = "DEPARTURE_TIME|PRICE|DURATION|AVAILABLE_SEATS",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of DEPARTURE_TIME, PRICE, DURATION, AVAILABLE_SEATS")
        String sortBy,

        @Pattern(
                regexp = "ASC|DESC",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be ASC or DESC")
        String sortDirection,

        String cursor,
        @Min(1) @Max(50) int size) {

    public SearchScheduledTripsRequest() {
        this(null, null, null, null, null, null, null, null, null, null, 20);
    }

    public SearchScheduledTripsQuery toQuery() {
        return new SearchScheduledTripsQuery(
                originStationId,
                destinationStationId,
                departureDate,
                normalize(status),
                Boolean.TRUE.equals(availableOnly),
                minPrice,
                maxPrice,
                ScheduledTripSearchSortField.from(sortBy),
                sortDirection == null || sortDirection.isBlank()
                        ? SortOrder.Direction.ASC
                        : SortOrder.Direction.valueOf(sortDirection.trim().toUpperCase()),
                normalizeCursor(cursor),
                size);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private static String normalizeCursor(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
