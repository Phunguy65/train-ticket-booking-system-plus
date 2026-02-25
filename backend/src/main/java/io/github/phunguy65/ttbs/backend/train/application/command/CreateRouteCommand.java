package io.github.phunguy65.ttbs.backend.train.application.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateRouteCommand(
        UUID trainId,
        UUID originStationId,
        UUID destinationStationId,
        Instant departureTime,
        Instant arrivalTime,
        BigDecimal basePrice) {}
