package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.UUID;

@Schema(
        description =
                "Full replacement payload for the authenticated customer profile. Send the complete editable profile state.")
public record UpdateAuthenticatedUserRequest(
        @Schema(description = "Updated full name.", example = "Nguyen Phuong")
        @NotBlank(message = "Full name is required") String fullName,

        @Schema(description = "Updated email address.", example = "customer@example.com")
        @Email(message = "Must be a valid email address") @NotBlank(message = "Email is required") String email,

        @Schema(description = "Updated phone number.", example = "+84901234567", nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Phone must not be blank") String phone,

        @Schema(
                description = "Updated date of birth.",
                type = "string",
                format = "date",
                nullable = true)
        LocalDate dateOfBirth,

        @Schema(
                description = "Updated self-declared gender label.",
                example = "female",
                nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Gender must not be blank") String gender,

        @Schema(
                description = "Updated government-issued identity document number.",
                writeOnly = true,
                example = "redacted-id-document",
                nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "ID document number must not be blank") String idDocumentNumber,

        @Schema(
                description = "Updated address line.",
                example = "redacted-address",
                nullable = true)
        @Pattern(regexp = ".*\\S.*", message = "Address line must not be blank") String addressLine) {

    public UpdateUserCommand toCommand(UUID userId) {
        return new UpdateUserCommand(
                UserId.of(userId),
                fullName,
                email,
                phone,
                dateOfBirth,
                gender,
                idDocumentNumber,
                addressLine);
    }
}
