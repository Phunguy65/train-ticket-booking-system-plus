package io.github.phunguy65.ttbs.backend.train.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Seat summary.")
public record SeatResponse(
        @Schema(description = "Seat identifier.", format = "uuid")
        UUID id,

        @Schema(description = "Parent coach identifier.", format = "uuid")
        UUID coachId,

        @Schema(description = "Seat label shown to customers.", example = "12A")
        String seatNumber,

        @Schema(description = "Seat creation timestamp.", format = "date-time")
        Instant createdAt) {}
