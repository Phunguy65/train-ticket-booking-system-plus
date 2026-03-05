package io.github.phunguy65.ttbs.backend.train.application.command;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import java.time.Instant;
import java.util.UUID;

public record CreateRouteCommand(
        UUID trainId,
        UUID originStationId,
        UUID destinationStationId,
        Instant departureTime,
        Instant arrivalTime,
        Money basePrice) {}
