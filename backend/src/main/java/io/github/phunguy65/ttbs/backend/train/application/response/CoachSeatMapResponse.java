package io.github.phunguy65.ttbs.backend.train.application.response;

import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import java.util.List;
import java.util.UUID;

public record CoachSeatMapResponse(UUID id, int carNumber, int totalSeats, List<Seat> seats) {

    public CoachSeatMapResponse {
        seats = List.copyOf(seats);
    }

    public record Seat(UUID id, String seatNumber, RouteSeatAvailabilityStatus status) {}
}
