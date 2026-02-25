package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record CreateRouteHttpRequest(
        @NotNull(message = "Train ID is required") UUID trainId,

        @NotNull(message = "Origin station ID is required") UUID originStationId,

        @NotNull(message = "Destination station ID is required") UUID destinationStationId,

        @NotNull(message = "Departure time is required") Instant departureTime,

        @NotNull(message = "Arrival time is required") Instant arrivalTime,

        @NotNull(message = "Base price is required") @Positive(message = "Base price must be positive") BigDecimal basePrice) {}
