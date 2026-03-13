package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID routeId,
        @NotEmpty List<UUID> seatIds,
        @NotBlank String passengerName,
        @Email @NotBlank String passengerEmail,
        String passengerPhone,
        @NotBlank String idempotencyKey) {}
