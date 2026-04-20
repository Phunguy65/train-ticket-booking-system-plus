package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Booking creation payload.")
public record CreateBookingRequest(
        @Schema(description = "Scheduled trip to book.", format = "uuid") @NotNull UUID scheduledTripId,

        @ArraySchema(
                schema = @Schema(description = "Seat identifier.", format = "uuid"),
                minItems = 1)
        @NotEmpty List<UUID> seatIds,

        @ArraySchema(schema = @Schema(implementation = PassengerInput.class), minItems = 1)
        @NotEmpty @Valid List<PassengerInput> passengers,

        @Schema(
                description =
                        "Client-generated idempotency key used to safely retry booking creation.",
                writeOnly = true,
                example = "booking-create-request-001")
        @jakarta.validation.constraints.NotBlank String idempotencyKey) {

    @Schema(description = "Passenger input for a booking seat.")
    public record PassengerInput(
            @Schema(description = "Seat identifier for this passenger.", format = "uuid") @NotNull UUID seatId,

            @Schema(description = "Passenger full name.", example = "Nguyen Van A") @NotBlank String fullName,

            @Schema(description = "Passenger identity document number.", example = "001234567890")
            @NotBlank String idDocumentNumber,

            @Schema(description = "Passenger date of birth.", type = "string", format = "date")
            @NotNull LocalDate dateOfBirth,

            @Schema(description = "Passenger gender.", example = "male") @NotBlank String gender) {}

    public CreateBookingCommand toCommand(UUID userId) {
        List<CreateBookingCommand.PassengerPayload> passengerPayloads = passengers.stream()
                .map(p -> new CreateBookingCommand.PassengerPayload(
                        SeatId.of(p.seatId()),
                        p.fullName(),
                        p.idDocumentNumber(),
                        p.dateOfBirth(),
                        p.gender()))
                .toList();

        return new CreateBookingCommand(
                userId,
                scheduledTripId,
                seatIds.stream().map(SeatId::of).toList(),
                passengerPayloads,
                idempotencyKey);
    }
}
