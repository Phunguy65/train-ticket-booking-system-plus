package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JSON serialization record for passenger data persisted alongside a booking.
 */
public record BookingPassengerSnapshotJson(
        UUID seatId,
        String fullName,
        String idDocumentNumber,
        LocalDate dateOfBirth,
        String gender) {}
