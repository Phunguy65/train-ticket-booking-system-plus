package io.github.phunguy65.ttbs.backend.train.domain.projection;

import java.util.UUID;

public record BookedSeatSummary(
        UUID seatId,
        UUID coachId,
        int coachNumber,
        String seatNumber,
        String status,
        Long priceAtBooking) {}
