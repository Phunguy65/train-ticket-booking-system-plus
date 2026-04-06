package io.github.phunguy65.ttbs.backend.user.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Customer profile returned by authenticated user endpoints.")
public record UserResponse(
        @Schema(
                description = "Customer identifier.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY)
        UUID id,

        @Schema(description = "Customer email address.", example = "customer@example.com")
        String email,

        @Schema(description = "Customer full name.", example = "Nguyen Phuong")
        String fullName,

        @Schema(description = "Customer phone number.", example = "+84901234567")
        String phone,

        @Schema(description = "Customer date of birth.", type = "string", format = "date")
        LocalDate dateOfBirth,

        @Schema(description = "Customer self-declared gender label.", example = "female")
        String gender,

        @Schema(
                description = "Government-issued identity document number on file.",
                example = "redacted-id-document")
        String idDocumentNumber,

        @Schema(description = "Primary address line on file.", example = "redacted-address")
        String addressLine,

        @Schema(description = "Customer role granted by the system.", example = "CUSTOMER")
        String role,

        @Schema(
                description = "Profile creation timestamp.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt) {}
