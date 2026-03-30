package io.github.phunguy65.ttbs.backend.booking.application.query;

import java.util.UUID;

public record GetBookingByIdQuery(UUID bookingId, UUID requestingUserId) {}
