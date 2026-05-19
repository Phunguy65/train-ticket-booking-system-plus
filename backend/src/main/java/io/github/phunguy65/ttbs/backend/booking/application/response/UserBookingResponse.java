package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Compact booking summary for booking-history listings.")
public record UserBookingResponse(
        @Schema(
                description = "Booking identifier.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY)
        UUID id,

        @Schema(description = "Customer identifier that owns the booking.", format = "uuid")
        UUID userId,

        @Schema(description = "Scheduled trip identifier.", format = "uuid")
        UUID scheduledTripId,

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
        Instant createdAt) {}
