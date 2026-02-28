package io.github.phunguy65.ttbs.backend.user.application.port;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;

/**
 * Cross-module port for validating booking constraints before deleting a user.
 *
 * <p>Owned by the {@code user} module — allows the {@code user} application layer to check
 * booking dependencies without coupling to booking JPA internals. The {@code booking} module
 * provides the implementation via {@code BookingValidationPortAdapter}.
 */
public interface BookingValidationPort {

    /**
     * Returns {@code true} if there are any non-cancelled bookings (HELD or CONFIRMED)
     * belonging to the given user.
     *
     * @param userId the user to check
     * @return {@code true} if deletion is blocked
     */
    boolean hasActiveBookingsForUser(UserId userId);
}
