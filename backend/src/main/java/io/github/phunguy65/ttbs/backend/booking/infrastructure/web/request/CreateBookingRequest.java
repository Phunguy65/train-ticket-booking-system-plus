package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Schema(description = "Booking creation payload.")
public record CreateBookingRequest(
        @Schema(description = "Scheduled trip to book.", format = "uuid") @NotNull UUID scheduledTripId,

        @ArraySchema(
                schema = @Schema(description = "Seat identifier.", format = "uuid"),
                minItems = 1)
        @NotEmpty List<UUID> seatIds,

        @Schema(
                description =
                        "Client-generated idempotency key used to safely retry booking creation.",
                writeOnly = true,
                example = "booking-create-request-001")
        @jakarta.validation.constraints.NotBlank String idempotencyKey) {

    public CreateBookingCommand toCommand(UUID userId) {
        return new CreateBookingCommand(
                userId, scheduledTripId, seatIds.stream().map(SeatId::of).toList(), idempotencyKey);
    }
}
