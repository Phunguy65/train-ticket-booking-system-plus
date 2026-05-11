package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse.Route;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed booking resource with trip, seat, and payment information.")
public record BookingDetailResponse(
        @Schema(
                description = "Booking identifier.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY)
        UUID id,

        @Schema(description = "Customer identifier that owns the booking.", format = "uuid")
        UUID userId,

        @Schema(description = "Scheduled trip identifier.", format = "uuid")
        UUID scheduledTripId,

        @Schema(description = "Booker (authenticated user) information stored with the booking.")
        PassengerInfoResponse bookerInfo,

        @ArraySchema(schema = @Schema(implementation = PassengerResponse.class))
        List<PassengerResponse> passengers,

        @Schema(description = "Booking total in minor currency units.", example = "650000")
        long totalPrice,

        @Schema(description = "ISO-like currency code.", example = "VND")
        String currency,

        @Schema(description = "Booking lifecycle status.") BookingStatus status,

        @Schema(
                description = "Deadline for completing payment before the booking expires.",
                format = "date-time")
        Instant paymentDeadline,

        @Schema(
                description = "Booking creation timestamp.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt,

        @Schema(description = "Scheduled trip summary linked to the booking.", nullable = true)
        Trip trip,

        @Schema(description = "Payment details linked to the booking.", nullable = true)
        PaymentDetailResponse payment,

        @ArraySchema(schema = @Schema(implementation = Seat.class))
        List<Seat> seats) {

    public BookingDetailResponse {
        passengers = passengers == null ? List.of() : List.copyOf(passengers);
        seats = seats == null ? List.of() : List.copyOf(seats);
    }

    @Schema(description = "Trip summary embedded inside a booking detail.")
    public record Trip(
            @Schema(description = "Scheduled trip identifier.", format = "uuid")
            UUID id,

            @Schema(description = "Route template identifier.", format = "uuid")
            UUID routeTemplateId,

            @Schema(description = "Train identifier.", format = "uuid")
            UUID trainId,

            @Schema(description = "Scheduled departure timestamp.", format = "date-time")
            Instant departureTime,

            @Schema(description = "Scheduled arrival timestamp.", format = "date-time")
            Instant arrivalTime,

            @Schema(description = "Scheduled trip status.") String status,

            @Schema(description = "Scheduled trip creation timestamp.", format = "date-time")
            Instant createdAt,

            @Schema(description = "Train summary.", nullable = true)
            Train train,

            @Schema(description = "Route summary.") Route route) {

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

    @Schema(description = "Seat reserved within a booking.")
    public record Seat(
            @Schema(description = "Seat identifier.", format = "uuid")
            UUID seatId,

            @Schema(description = "Coach identifier.", format = "uuid")
            UUID coachId,

            @Schema(description = "Coach car number.", example = "5")
            int coachNumber,

            @Schema(description = "Seat label shown to customers.", example = "12A")
            String seatNumber,

            @Schema(description = "Seat status at the time the booking detail is viewed.")
            RouteSeatAvailabilityStatus status,

            @Schema(
                    description = "Seat price captured at booking time in minor currency units.",
                    example = "325000")
            Long priceAtBooking) {}
}
