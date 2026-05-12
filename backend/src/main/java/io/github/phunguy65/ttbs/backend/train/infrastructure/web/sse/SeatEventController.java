package io.github.phunguy65.ttbs.backend.train.infrastructure.web.sse;

import io.github.phunguy65.ttbs.backend.shared.domain.event.SeatStatusChangedEvent;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.application.sse.SeatEventBroadcaster;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.swagger.v3.oas.annotations.Hidden;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE controller for real-time seat status updates.
 *
 * <p>Endpoint: {@code GET /v1/sse/trips/{scheduledTripId}/seats}
 * Requires a valid JWT — authenticated via Spring Security's filter chain
 * (see {@code SecurityConfig}). Unauthenticated requests are rejected at the
 * security filter level before reaching this controller.
 *
 * <p>Uses {@link RouteSeatAvailabilityManager} to fetch the initial seat state,
 * respecting the layer boundary (controller → application port → domain repository).
 */
@RestController
@Hidden
@RequestMapping
public class SeatEventController {

    private final SeatEventBroadcaster broadcaster;
    private final RouteSeatAvailabilityManager seatAvailabilityManager;

    public SeatEventController(
            SeatEventBroadcaster broadcaster,
            RouteSeatAvailabilityManager seatAvailabilityManager) {
        this.broadcaster = broadcaster;
        this.seatAvailabilityManager = seatAvailabilityManager;
    }

    /**
     * Opens an SSE stream for real-time seat status updates on a specific scheduled trip.
     *
     * <p>On connect, sends a {@code seat-initial} event containing the full seat map
     * (all statuses). Subsequent changes are sent as {@code seat-changed} events.
     *
     * <p>Authentication is handled by Spring Security. The security filter chain runs
     * before this controller, populating the Security Context with the authenticated user.
     * If no valid authentication is present, the request is rejected with HTTP 401.
     *
     * @param scheduledTripId the trip to subscribe to
     * @return an open SSE emitter
     */
    @GetMapping(
            value = "/{version}/sse/trips/{scheduledTripId}/seats",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE,
            version = "1.0")
    public SseEmitter subscribe(@PathVariable UUID scheduledTripId) {

        SseEmitter emitter = broadcaster.subscribe(scheduledTripId);

        sendInitialState(scheduledTripId, emitter);

        return emitter;
    }

    private void sendInitialState(UUID scheduledTripId, SseEmitter emitter) {
        try {
            ScheduledTripId tripId = ScheduledTripId.of(scheduledTripId);
            List<RouteSeatAvailability> seats =
                    seatAvailabilityManager.findAllByScheduledTripId(tripId);

            List<SeatStatusChangedEvent.SeatChange> seatChanges = seats.stream()
                    .map(seat -> new SeatStatusChangedEvent.SeatChange(
                            seat.getSeatId().value(), seat.getStatus().name(), seat.getBookingId()))
                    .toList();

            SeatStatusChangedEvent initialEvent =
                    new SeatStatusChangedEvent(scheduledTripId, seatChanges, Instant.now());

            emitter.send(SseEmitter.event().name("seat-initial").data(initialEvent));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }
}
