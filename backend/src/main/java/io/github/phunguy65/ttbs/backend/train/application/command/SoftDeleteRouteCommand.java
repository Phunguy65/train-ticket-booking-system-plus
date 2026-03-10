package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;

public record SoftDeleteRouteCommand(RouteId routeId) {}
