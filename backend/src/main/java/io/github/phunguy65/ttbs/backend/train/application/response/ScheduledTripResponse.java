package io.github.phunguy65.ttbs.backend.train.application.response;

import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Scheduled trip summary.")
public record ScheduledTripResponse(
        @Schema(
                description = "Scheduled trip identifier.",
                format = "uuid",
                example = "11111111-1111-1111-1111-111111111111")
        UUID id,

        @Schema(
                description = "Route template identifier.",
                format = "uuid",
                example = "22222222-2222-2222-2222-222222222222")
        UUID routeTemplateId,

        @Schema(
                description = "Train identifier.",
                format = "uuid",
                example = "33333333-3333-3333-3333-333333333333")
        UUID trainId,

        @Schema(
                description = "Scheduled departure timestamp.",
                format = "date-time",
                example = "2026-05-01T01:00:00Z")
        Instant departureTime,

        @Schema(
                description = "Scheduled arrival timestamp.",
                format = "date-time",
                example = "2026-05-01T11:00:00Z")
        Instant arrivalTime,

        @Schema(description = "Scheduled trip lifecycle status.")
        ScheduledTripStatus status,

        @Schema(
                description = "Scheduled trip creation timestamp.",
                format = "date-time",
                example = "2026-04-01T10:15:30Z")
        Instant createdAt) {}
