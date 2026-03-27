package io.github.phunguy65.ttbs.backend.user.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateUserRequest(
        @NotBlank JsonNullable<String> fullName,
        @NotBlank @Email JsonNullable<String> email,
        JsonNullable<String> phone) {

    UpdateUserRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }

    public UpdateUserCommand toCommand(UUID userId) {
        return new UpdateUserCommand(UserId.of(userId), fullName, email, phone);
    }
}
