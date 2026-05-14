package io.github.phunguy65.ttbs.backend.payment.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Enriched payment detail with ticket-ready booking information.")
public record PaymentDetailResponse(
        @Schema(description = "Payment identifier.", format = "uuid")
        UUID paymentId,

        @Schema(description = "Booking identifier linked to the payment.", format = "uuid")
        UUID bookingId,

        @Schema(description = "Payment lifecycle status.") PaymentStatus status,

        @Schema(
                description = "Hosted checkout URL when customer action is still required.",
                format = "uri",
                nullable = true,
                types = {"string", "null"})
        String checkoutUrl,

        @Schema(description = "Payment amount in major currency units.", example = "650000")
        BigDecimal amount,

        @Schema(description = "ISO-like currency code.", example = "VND")
        String currency,

        @Schema(
                description = "Payment creation timestamp.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt,

        @Schema(description = "Ticket-ready booking summary for printing.", nullable = true)
        BookingForTicket booking) {

    @Schema(description = "Booking summary with ticket-ready data for printing.")
    public record BookingForTicket(
            @Schema(description = "Booking identifier.", format = "uuid")
            UUID id,

            @Schema(description = "Booking lifecycle status.")
            BookingStatus status,

            @Schema(
                    description =
                            "Booker (authenticated user) information stored with the booking.")
            BookerInfo bookerInfo,

            @ArraySchema(schema = @Schema(implementation = PassengerWithSeat.class))
            List<PassengerWithSeat> passengers,

            @ArraySchema(schema = @Schema(implementation = SeatInfo.class))
            List<SeatInfo> seats,

            @Schema(description = "Trip data with train and route information.", nullable = true)
            TripInfo trip) {

        public BookingForTicket {
            passengers = passengers == null ? List.of() : List.copyOf(passengers);
            seats = seats == null ? List.of() : List.copyOf(seats);
        }
    }

    @Schema(description = "Booker (authenticated user) identity information.")
    public record BookerInfo(
            @Schema(description = "Booker full name.", example = "Nguyen Phuong")
            String fullName,

            @Schema(description = "Booker email address.", example = "customer@example.com")
            String email,

            @Schema(
                    description = "Booker phone number.",
                    example = "+84901234567",
                    nullable = true,
                    types = {"string", "null"})
            String phone,

            @Schema(
                    description = "Booker date of birth.",
                    type = "string",
                    format = "date",
                    nullable = true,
                    types = {"string", "null"})
            LocalDate dateOfBirth,

            @Schema(
                    description = "Booker self-declared gender label.",
                    example = "female",
                    nullable = true,
                    types = {"string", "null"})
            String gender,

            @Schema(
                    description = "Booker government-issued identity document number.",
                    example = "redacted-id-document",
                    nullable = true,
                    types = {"string", "null"})
            String idDocumentNumber) {}

    @Schema(description = "Passenger with seat assignment for ticket printing.")
    public record PassengerWithSeat(
            @Schema(description = "Seat identifier.", format = "uuid")
            UUID seatId,

            @Schema(description = "Coach car number.", example = "5")
            int coachNumber,

            @Schema(description = "Seat label shown to customers.", example = "12A")
            String seatNumber,

            @Schema(description = "Passenger full name.", example = "Nguyen Van A")
            String fullName,

            @Schema(
                    description = "Passenger identity document number.",
                    example = "001234567890",
                    nullable = true,
                    types = {"string", "null"})
            String idDocumentNumber,

            @Schema(
                    description = "Passenger date of birth.",
                    type = "string",
                    format = "date",
                    nullable = true,
                    types = {"string", "null"})
            LocalDate dateOfBirth,

            @Schema(
                    description = "Passenger gender.",
                    example = "male",
                    nullable = true,
                    types = {"string", "null"})
            String gender) {}

    @Schema(description = "Seat information for ticket printing.")
    public record SeatInfo(
            @Schema(description = "Seat identifier.", format = "uuid")
            UUID seatId,

            @Schema(description = "Coach car number.", example = "5")
            int coachNumber,

            @Schema(description = "Seat label shown to customers.", example = "12A")
            String seatNumber) {}

    @Schema(description = "Trip data with train and route information for ticket printing.")
    public record TripInfo(
            @Schema(description = "Train name.", example = "SE1")
            String trainName,

            @Schema(description = "Train number.", example = "SE001")
            String trainNumber,

            @Schema(description = "Origin station name.", example = "Hanoi")
            String origin,

            @Schema(description = "Destination station name.", example = "Ho Chi Minh City")
            String destination,

            @Schema(description = "Scheduled departure timestamp.", format = "date-time")
            Instant departureTime,

            @Schema(description = "Scheduled arrival timestamp.", format = "date-time")
            Instant arrivalTime) {}
}
