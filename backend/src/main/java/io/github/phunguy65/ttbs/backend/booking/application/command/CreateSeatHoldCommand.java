package io.github.phunguy65.ttbs.backend.booking.application.command;

import java.util.List;
import java.util.UUID;

/**
 * Command for creating a multi-seat hold.
 *
 * @param userId         the user creating the hold
 * @param routeId        the route for which seats are being held
 * @param seatIds        list of seat IDs to hold (must be non-empty)
 * @param idempotencyKey unique key for deduplication
 * @param passengerName  passenger name
 * @param passengerEmail passenger email
 * @param passengerPhone passenger phone (nullable)
 */
public record CreateSeatHoldCommand(
        UUID userId,
        UUID routeId,
        List<UUID> seatIds,
        String idempotencyKey,
        String passengerName,
        String passengerEmail,
        String passengerPhone) {}
