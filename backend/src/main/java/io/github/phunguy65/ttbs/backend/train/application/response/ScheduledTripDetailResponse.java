package io.github.phunguy65.ttbs.backend.train.application.response;

import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus;
import java.time.Instant;
import java.util.UUID;

public record ScheduledTripDetailResponse(
        UUID id,
        UUID routeTemplateId,
        UUID trainId,
        Instant departureTime,
        Instant arrivalTime,
        ScheduledTripStatus status,
        Instant createdAt,
        Train train,
        Route route) {

    public record Train(UUID id, String trainNumber, String name, int totalSeats) {}

    public record Route(
            UUID id, long basePrice, String currency, Station origin, Station destination) {}

    public record Station(UUID id, String code, String name, String city) {}
}
