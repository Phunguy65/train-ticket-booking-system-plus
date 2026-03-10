package io.github.phunguy65.ttbs.backend.booking.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.List;
import java.util.UUID;

public record CreateBookingCommand(
        UUID userId,
        UUID routeId,
        List<SeatId> seatIds,
        String passengerName,
        String passengerEmail,
        String passengerPhone,
        String idempotencyKey) {}
