package io.github.phunguy65.ttbs.backend.train.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record ScheduledTripEnrichedSummary(
        UUID id,
        UUID routeTemplateId,
        UUID trainId,
        Instant departureTime,
        Instant arrivalTime,
        String status,
        Instant createdAt,
        long durationMinutes,
        long availableSeatCount,
        String trainNumber,
        String trainName,
        Integer trainTotalSeats,
        UUID originStationId,
        String originStationCode,
        String originStationName,
        String originStationCity,
        UUID destinationStationId,
        String destinationStationCode,
        String destinationStationName,
        String destinationStationCity,
        long routeBasePrice,
        String routeCurrency) {}
