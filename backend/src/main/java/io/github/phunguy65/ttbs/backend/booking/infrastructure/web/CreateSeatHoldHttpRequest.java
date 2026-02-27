package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * HTTP request body for {@code POST /api/{version}/bookings/hold}.
 *
 * @param userId         the ID of the user creating the hold
 * @param routeId        the route for which seats are being held
 * @param seatIds        non-empty list of seat IDs to hold
 * @param idempotencyKey unique key for deduplication (client-provided)
 * @param passengerName  full name of the passenger
 * @param passengerEmail contact email of the passenger
 * @param passengerPhone contact phone (optional)
 */
public record CreateSeatHoldHttpRequest(
        @NotNull UUID userId,
        @NotNull UUID routeId,
        @NotEmpty List<UUID> seatIds,
        @NotBlank String idempotencyKey,
        @NotBlank String passengerName,
        @NotBlank @Email String passengerEmail,
        String passengerPhone) {}
