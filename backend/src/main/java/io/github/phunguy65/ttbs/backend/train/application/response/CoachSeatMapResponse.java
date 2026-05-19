package io.github.phunguy65.ttbs.backend.train.application.response;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Seat map for a coach on a scheduled trip.")
public record CoachSeatMapResponse(
        @Schema(description = "Coach identifier.", format = "uuid")
        UUID id,

        @Schema(description = "Coach car number within the train.", example = "5")
        int carNumber,

        @Schema(description = "Total seats in the coach.", example = "64")
        int totalSeats,

        @ArraySchema(schema = @Schema(implementation = Seat.class))
        List<Seat> seats) {

    public CoachSeatMapResponse {
        seats = List.copyOf(seats);
    }

    @Schema(description = "Seat state inside a coach seat map.")
    public record Seat(
            @Schema(description = "Seat identifier.", format = "uuid")
            UUID id,

            @Schema(description = "Seat label shown to customers.", example = "12A")
            String seatNumber,

            @Schema(description = "Current seat availability status.")
            RouteSeatAvailabilityStatus status) {}
}
