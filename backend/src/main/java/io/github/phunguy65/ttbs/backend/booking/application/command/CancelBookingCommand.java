package io.github.phunguy65.ttbs.backend.booking.application.command;

import java.util.UUID;

public record CancelBookingCommand(UUID bookingId, UUID requestingUserId) {}
