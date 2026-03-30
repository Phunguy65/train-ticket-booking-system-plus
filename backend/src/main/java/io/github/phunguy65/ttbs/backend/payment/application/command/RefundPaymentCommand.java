package io.github.phunguy65.ttbs.backend.payment.application.command;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;

public record RefundPaymentCommand(BookingId bookingId) {}
