package io.github.phunguy65.ttbs.backend.train.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Coach summary belonging to a train.")
public record CoachResponse(
        @Schema(description = "Coach identifier.", format = "uuid")
        UUID id,

        @Schema(description = "Parent train identifier.", format = "uuid")
        UUID trainId,

        @Schema(description = "Coach car number within the train.", example = "5")
        int carNumber,

        @Schema(description = "Total seats in the coach.", example = "64")
        int totalSeats,

        @Schema(description = "Coach creation timestamp.", format = "date-time")
        Instant createdAt) {}
