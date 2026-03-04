package io.github.phunguy65.ttbs.backend.booking.application.command;

import java.util.UUID;

/**
 * Command for confirming a held booking after payment.
 *
 * @param bookingId the ID of the booking to confirm
 */
public record ConfirmSeatHoldCommand(UUID bookingId) {}
