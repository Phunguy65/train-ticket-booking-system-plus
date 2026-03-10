package io.github.phunguy65.ttbs.backend.train.application.dto;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import java.time.Instant;
import java.util.UUID;

public record RouteDto(
        UUID id,
        UUID trainId,
        UUID originStationId,
        UUID destinationStationId,
        Instant departureTime,
        Instant arrivalTime,
        long basePrice,
        RouteStatus status,
        Instant createdAt) {}
