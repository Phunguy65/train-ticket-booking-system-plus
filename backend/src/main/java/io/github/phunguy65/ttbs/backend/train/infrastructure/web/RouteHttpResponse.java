package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import java.time.Instant;
import java.util.UUID;

record RouteHttpResponse(
        UUID id,
        UUID trainId,
        UUID originStationId,
        UUID destinationStationId,
        Instant departureTime,
        Instant arrivalTime,
        long basePrice,
        RouteStatus status,
        Instant createdAt) {}
