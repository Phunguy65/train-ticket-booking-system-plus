package io.github.phunguy65.ttbs.backend.user.application.command;

import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateUserCommand(
        UserId userId,
        JsonNullable<String> fullName,
        JsonNullable<String> email,
        JsonNullable<String> phone) {}
