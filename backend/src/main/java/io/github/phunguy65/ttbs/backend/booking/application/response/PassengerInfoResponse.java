package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Passenger identity and contact information captured for a booking.")
public record PassengerInfoResponse(
        @Schema(description = "Passenger full name.", example = "Nguyen Phuong")
        String fullName,

        @Schema(description = "Passenger email address.", example = "customer@example.com")
        String email,

        @Schema(
                description = "Passenger phone number.",
                example = "+84901234567",
                nullable = true,
                types = {"string", "null"})
        String phone,

        @Schema(
                description = "Passenger date of birth.",
                type = "string",
                format = "date",
                nullable = true,
                types = {"string", "null"})
        LocalDate dateOfBirth,

        @Schema(
                description = "Passenger self-declared gender label.",
                example = "female",
                nullable = true,
                types = {"string", "null"})
        String gender,

        @Schema(
                description = "Passenger government-issued identity document number.",
                example = "redacted-id-document",
                nullable = true,
                types = {"string", "null"})
        String idDocumentNumber,

        @Schema(
                description = "Passenger address line.",
                example = "redacted-address",
                nullable = true,
                types = {"string", "null"})
        String addressLine) {}
