package io.github.phunguy65.ttbs.backend.user.application.command;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.LocalDate;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateUserCommand(
        UserId userId,
        JsonNullable<String> fullName,
        JsonNullable<String> email,
        JsonNullable<String> phone,
        JsonNullable<LocalDate> dateOfBirth,
        JsonNullable<String> gender,
        JsonNullable<String> idDocumentNumber,
        JsonNullable<String> addressLine) {}
