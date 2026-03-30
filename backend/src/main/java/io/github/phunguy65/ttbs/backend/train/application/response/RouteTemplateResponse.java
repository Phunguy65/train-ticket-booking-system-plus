package io.github.phunguy65.ttbs.backend.train.application.response;

import java.time.Instant;
import java.util.UUID;

public record RouteTemplateResponse(
        UUID id,
        UUID originStationId,
        UUID destinationStationId,
        long basePrice,
        String currency,
        Instant createdAt) {}
