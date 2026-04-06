package io.github.phunguy65.ttbs.backend.train.application.response;

import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Search result row for cursor-based scheduled trip discovery.")
public record SearchScheduledTripsResponse(
        @Schema(description = "Scheduled trip identifier.", format = "uuid")
        UUID id,

        @Schema(description = "Scheduled departure timestamp.", format = "date-time")
        Instant departureTime,

        @Schema(description = "Scheduled arrival timestamp.", format = "date-time")
        Instant arrivalTime,

        @Schema(description = "Scheduled trip lifecycle status.")
        ScheduledTripStatus status,

        @Schema(description = "Trip duration in minutes.", example = "600")
        long durationMinutes,

        @Schema(description = "Currently available seat count.", example = "8")
        long availableSeatCount,

        @Schema(description = "Current occupancy percentage.", example = "75")
        int occupancyPercentage,

        @Schema(description = "Train summary.") Train train,
        @Schema(description = "Route summary.") Route route) {

    @Schema(description = "Train summary embedded inside a search result.")
    public record Train(
            @Schema(description = "Train identifier.", format = "uuid")
            UUID id,

            @Schema(description = "Train number.", example = "SE1")
            String trainNumber,

            @Schema(description = "Train display name.", example = "Reunification Express")
            String name,

            @Schema(description = "Total seats available on the train.", example = "320")
            Integer totalSeats) {}

    @Schema(description = "Route summary embedded inside a search result.")
    public record Route(
            @Schema(description = "Route template identifier.", format = "uuid")
            UUID id,

            @Schema(description = "Base fare in minor currency units.", example = "650000")
            long basePrice,

            @Schema(description = "ISO-like currency code.", example = "VND")
            String currency,

            @Schema(description = "Origin station summary.") Station origin,

            @Schema(description = "Destination station summary.")
            Station destination) {}

    @Schema(description = "Station summary embedded inside a route.")
    public record Station(
            @Schema(description = "Station identifier.", format = "uuid")
            UUID id,

            @Schema(description = "Station code.", example = "HNO")
            String code,

            @Schema(description = "Station name.", example = "Ha Noi")
            String name,

            @Schema(description = "Station city.", example = "Ha Noi")
            String city) {}
}
