package io.github.phunguy65.ttbs.backend.booking.application.command;

import java.util.UUID;

/**
 * Command for confirming a held booking after payment.
 *
 * @param bookingId        the ID of the booking to confirm
 * @param paymentReference the payment reference from the payment provider
 */
public record ConfirmSeatHoldCommand(UUID bookingId, String paymentReference) {}
