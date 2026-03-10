package io.github.phunguy65.ttbs.backend.user.application.command;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.List;

public record BulkSoftDeleteUsersCommand(List<UserId> userIds) {}
