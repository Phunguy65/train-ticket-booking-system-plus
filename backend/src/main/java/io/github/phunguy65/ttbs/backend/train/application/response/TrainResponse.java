package io.github.phunguy65.ttbs.backend.train.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Train summary.")
public record TrainResponse(
        @Schema(description = "Train identifier.", format = "uuid")
        UUID id,

        @Schema(description = "Train number visible to operators and customers.", example = "SE1")
        String trainNumber,

        @Schema(description = "Train display name.", example = "Reunification Express")
        String name,

        @Schema(description = "Total number of seats across the train.", example = "320")
        int totalSeats,

        @Schema(description = "Train creation timestamp.", format = "date-time")
        Instant createdAt) {}
