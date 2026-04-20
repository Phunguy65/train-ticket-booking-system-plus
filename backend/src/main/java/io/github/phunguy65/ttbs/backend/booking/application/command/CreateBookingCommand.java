package io.github.phunguy65.ttbs.backend.booking.application.command;

import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateBookingCommand(
        UUID userId,
        UUID scheduledTripId,
        List<SeatId> seatIds,
        List<PassengerPayload> passengers,
        String idempotencyKey) {

    /**
     * Passenger payload data from the web layer.
     */
    public record PassengerPayload(
            SeatId seatId,
            String fullName,
            String idDocumentNumber,
            LocalDate dateOfBirth,
            String gender) {}
}
