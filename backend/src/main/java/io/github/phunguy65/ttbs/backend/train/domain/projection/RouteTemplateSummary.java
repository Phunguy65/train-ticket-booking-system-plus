package io.github.phunguy65.ttbs.backend.train.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record RouteTemplateSummary(
        UUID id,
        UUID originStationId,
        UUID destinationStationId,
        long basePrice,
        String currency,
        Instant createdAt) {}
