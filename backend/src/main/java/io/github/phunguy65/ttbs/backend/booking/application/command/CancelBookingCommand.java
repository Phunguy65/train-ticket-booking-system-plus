package io.github.phunguy65.ttbs.backend.booking.application.command;

import java.util.UUID;

/**
 * Command for cancelling a booking (HELD or CONFIRMED).
 *
 * @param bookingId the ID of the booking to cancel
 */
public record CancelBookingCommand(UUID bookingId) {}
