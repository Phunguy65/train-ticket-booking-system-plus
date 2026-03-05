package io.github.phunguy65.ttbs.backend.booking.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Port exposing stale hold query capability to external modules (e.g. payment reconciliation).
 * Part of the booking::api named interface.
 */
public interface StaleHoldQueryPort {

    record StaleHoldView(UUID bookingId, String checkoutSessionId) {}

    List<StaleHoldView> findStaleHoldsWithCheckoutSession(Instant threshold);
}
