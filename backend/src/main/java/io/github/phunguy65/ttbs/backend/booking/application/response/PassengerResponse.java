package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Passenger assigned to a specific seat in a booking.
 */
@Schema(description = "Passenger assigned to a seat in a booking.")
public record PassengerResponse(
        @Schema(description = "Seat identifier.", format = "uuid")
        UUID seatId,

        @Schema(description = "Passenger full name.", example = "Nguyen Van A")
        String fullName,

        @Schema(description = "Passenger identity document number.", example = "001234567890")
        String idDocumentNumber,

        @Schema(description = "Passenger date of birth.", type = "string", format = "date")
        LocalDate dateOfBirth,

        @Schema(description = "Passenger gender.", example = "male")
        String gender) {}
