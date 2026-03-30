package io.github.phunguy65.ttbs.backend.train.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record ScheduledTripSummary(
        UUID id,
        UUID routeTemplateId,
        UUID trainId,
        Instant departureTime,
        Instant arrivalTime,
        String status,
        Instant createdAt) {}
