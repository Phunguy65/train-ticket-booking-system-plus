package io.github.phunguy65.ttbs.backend.train.application.response;

import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import java.time.Instant;
import java.util.UUID;

public record ScheduledTripResponse(
        UUID id,
        UUID routeTemplateId,
        UUID trainId,
        Instant departureTime,
        Instant arrivalTime,
        ScheduledTripStatus status,
        Instant createdAt) {}
