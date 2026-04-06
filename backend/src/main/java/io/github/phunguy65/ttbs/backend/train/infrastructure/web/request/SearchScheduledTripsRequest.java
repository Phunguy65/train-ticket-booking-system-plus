package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.train.application.query.ScheduledTripSearchSortField;
import io.github.phunguy65.ttbs.backend.train.application.query.SearchScheduledTripsQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "Cursor-based search query for scheduled trips.")
public record SearchScheduledTripsRequest(
        @Schema(description = "Origin station filter.", format = "uuid")
        UUID originStationId,

        @Schema(description = "Destination station filter.", format = "uuid")
        UUID destinationStationId,

        @Schema(description = "Departure date filter.", type = "string", format = "date")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate departureDate,

        @Schema(
                description = "Scheduled trip status filter.",
                allowableValues = {"SCHEDULED", "BOARDING", "DEPARTED", "ARRIVED", "CANCELLED"})
        @Pattern(
                regexp = "SCHEDULED|BOARDING|DEPARTED|ARRIVED|CANCELLED",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of SCHEDULED, BOARDING, DEPARTED, ARRIVED, CANCELLED")
        String status,

        @Schema(
                description = "When true, only trips with currently available seats are returned.",
                example = "true")
        Boolean availableOnly,

        @Schema(
                description = "Minimum fare filter in minor currency units.",
                minimum = "0",
                example = "350000")
        @Min(0) Long minPrice,

        @Schema(
                description = "Maximum fare filter in minor currency units.",
                minimum = "0",
                example = "950000")
        @Min(0) Long maxPrice,

        @Schema(
                description = "Field used to sort results.",
                allowableValues = {"DEPARTURE_TIME", "PRICE", "DURATION", "AVAILABLE_SEATS"})
        @Pattern(
                regexp = "DEPARTURE_TIME|PRICE|DURATION|AVAILABLE_SEATS",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be one of DEPARTURE_TIME, PRICE, DURATION, AVAILABLE_SEATS")
        String sortBy,

        @Schema(
                description = "Sort direction.",
                allowableValues = {"ASC", "DESC"})
        @Pattern(
                regexp = "ASC|DESC",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "must be ASC or DESC")
        String sortDirection,

        @Schema(
                description =
                        "Opaque cursor from the previous search page. Omit to start from the beginning.",
                example = "opaque-cursor-token")
        String cursor,

        @Schema(
                description = "Number of results per page.",
                minimum = "1",
                maximum = "50",
                example = "20")
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
