package io.github.phunguy65.ttbs.backend.payment.application.command;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;

public record CreateCheckoutSessionCommand(
        BookingId bookingId,
        UserId userId,
        Money amount,
        String currency,
        String successUrl,
        String cancelUrl) {}
