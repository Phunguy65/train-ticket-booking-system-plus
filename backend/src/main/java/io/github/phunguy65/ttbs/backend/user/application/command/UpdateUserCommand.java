package io.github.phunguy65.ttbs.backend.user.application.command;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateUserCommand(
        UserId userId,
        JsonNullable<String> fullName,
        JsonNullable<String> email,
        JsonNullable<String> phone) {}
