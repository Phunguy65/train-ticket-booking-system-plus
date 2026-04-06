package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;

@Schema(
        description =
                "Patch payload for the authenticated customer profile. Omit a field to leave it unchanged.")
public record UpdateAuthenticatedUserRequest(
        @Schema(description = "Updated full name.", example = "Nguyen Phuong") @NotBlank JsonNullable<String> fullName,

        @Schema(description = "Updated email address.", example = "customer@example.com")
        @NotBlank @Email JsonNullable<String> email,

        @Schema(description = "Updated phone number.", example = "+84901234567")
        JsonNullable<String> phone,

        @Schema(description = "Updated date of birth.", type = "string", format = "date")
        JsonNullable<LocalDate> dateOfBirth,

        @Schema(description = "Updated self-declared gender label.", example = "female")
        JsonNullable<String> gender,

        @Schema(
                description = "Updated government-issued identity document number.",
                writeOnly = true,
                example = "redacted-id-document")
        JsonNullable<String> idDocumentNumber,

        @Schema(description = "Updated address line.", example = "redacted-address")
        JsonNullable<String> addressLine) {

    UpdateAuthenticatedUserRequest() {
        this(
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined(),
                JsonNullable.undefined());
    }

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
