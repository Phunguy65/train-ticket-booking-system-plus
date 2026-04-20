package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Booking resource returned after successful booking creation.")
public record BookingResponse(
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
        Instant createdAt) {

    public BookingResponse {
        passengers = passengers == null ? List.of() : List.copyOf(passengers);
    }
}
