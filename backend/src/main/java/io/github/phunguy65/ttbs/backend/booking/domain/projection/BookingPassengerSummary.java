package io.github.phunguy65.ttbs.backend.booking.domain.projection;

import java.time.LocalDate;
import java.util.UUID;

public record BookingPassengerSummary(
        UUID seatId,
        String fullName,
        String idDocumentNumber,
        LocalDate dateOfBirth,
        String gender) {}
