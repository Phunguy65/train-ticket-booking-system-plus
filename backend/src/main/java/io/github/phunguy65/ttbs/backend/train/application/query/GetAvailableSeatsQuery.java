package io.github.phunguy65.ttbs.backend.train.application.query;

import java.util.UUID;

public record GetAvailableSeatsQuery(int page, int size, UUID routeId) {}
