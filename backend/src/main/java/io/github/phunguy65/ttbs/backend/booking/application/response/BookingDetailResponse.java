package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse.Route;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingDetailResponse(
        UUID id,
        UUID userId,
        UUID scheduledTripId,
        PassengerInfoResponse passengerInfo,
        long totalPrice,
        String currency,
        BookingStatus status,
        Instant paymentDeadline,
        Instant createdAt,
        Trip trip,
        PaymentDetailResponse payment,
        List<Seat> seats) {

    public BookingDetailResponse {
        seats = List.copyOf(seats);
    }

    public record Trip(
            UUID id,
            UUID routeTemplateId,
            UUID trainId,
            Instant departureTime,
            Instant arrivalTime,
            String status,
            Instant createdAt,
            Train train,
            Route route) {

        public static Trip fromScheduledTripDetail(ScheduledTripDetailResponse response) {
            return new Trip(
                    response.id(),
                    response.routeTemplateId(),
                    response.trainId(),
                    response.departureTime(),
                    response.arrivalTime(),
                    response.status().name(),
                    response.createdAt(),
                    response.train(),
                    response.route());
        }
    }

    public record Seat(
            UUID seatId,
            UUID coachId,
            int coachNumber,
            String seatNumber,
            RouteSeatAvailabilityStatus status,
            Long priceAtBooking) {}
}
