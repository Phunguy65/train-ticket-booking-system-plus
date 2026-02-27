package io.github.phunguy65.ttbs.backend.user.application.command;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;

public record SoftDeleteUserCommand(UserId userId) {}
