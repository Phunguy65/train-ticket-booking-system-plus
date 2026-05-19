package io.github.phunguy65.ttbs.backend.payment.application.query;

import java.util.UUID;

public record GetPaymentByBookingIdQuery(UUID bookingId, UUID requestingUserId) {}
