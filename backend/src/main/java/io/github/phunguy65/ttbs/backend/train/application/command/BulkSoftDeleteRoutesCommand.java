package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.util.List;

public record BulkSoftDeleteRoutesCommand(List<RouteId> routeIds) {}
