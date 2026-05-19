package io.github.phunguy65.ttbs.backend.train.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Route template summary describing the default fare between two stations.")
public record RouteTemplateResponse(
        @Schema(description = "Route template identifier.", format = "uuid")
        UUID id,

        @Schema(description = "Origin station identifier.", format = "uuid")
        UUID originStationId,

        @Schema(description = "Destination station identifier.", format = "uuid")
        UUID destinationStationId,

        @Schema(description = "Default fare in minor currency units.", example = "650000")
        long basePrice,

        @Schema(description = "ISO-like currency code.", example = "VND")
        String currency,

        @Schema(description = "Route template creation timestamp.", format = "date-time")
        Instant createdAt) {}
