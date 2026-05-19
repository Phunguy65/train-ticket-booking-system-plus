package io.github.phunguy65.ttbs.backend.train.application.sse;

import io.github.phunguy65.ttbs.backend.shared.domain.event.SeatStatusChangedEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Singleton service managing SSE emitter subscriptions per scheduled trip.
 *
 * <p>Thread-safe via {@code ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>}.
 * Emitters are automatically removed on completion, timeout, or error.
 */
@Service
public class SeatEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SeatEventBroadcaster.class);

    /** Subscribers per scheduled trip. CopyOnWriteArrayList avoids locking on read-heavy workloads. */
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> subscribers =
            new ConcurrentHashMap<>();

    /**
     * Subscribes a new SSE emitter for the given scheduled trip.
     * Registers cleanup callbacks so dead emitters are removed automatically.
     */
    public SseEmitter subscribe(UUID scheduledTripId) {
        SseEmitter emitter = new SseEmitter(0L);
        CopyOnWriteArrayList<SseEmitter> tripSubs =
                subscribers.computeIfAbsent(scheduledTripId, k -> new CopyOnWriteArrayList<>());
        tripSubs.add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for scheduledTripId={}", scheduledTripId);
            remove(scheduledTripId, emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE timed out for scheduledTripId={}", scheduledTripId);
            remove(scheduledTripId, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE error for scheduledTripId={}: {}", scheduledTripId, e.getMessage());
            remove(scheduledTripId, emitter);
        });

        log.info(
                "SSE subscriber added for scheduledTripId={}, total={}",
                scheduledTripId,
                tripSubs.size());
        return emitter;
    }

    /**
     * Broadcasts a seat status change event to all subscribers of the given scheduled trip.
     * Fire-and-forget: exceptions are logged at WARN but do not propagate.
     */
    public void broadcast(UUID scheduledTripId, SeatStatusChangedEvent event) {
        List<SseEmitter> dead = new java.util.ArrayList<>();
        CopyOnWriteArrayList<SseEmitter> tripSubs = subscribers.get(scheduledTripId);
        if (tripSubs == null || tripSubs.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : tripSubs) {
            try {
                emitter.send(SseEmitter.event().name("seat-changed").data(event));
            } catch (IOException e) {
                log.warn(
                        "Failed to send SSE event to client for scheduledTripId={}: {}",
                        scheduledTripId,
                        e.getMessage());
                dead.add(emitter);
            }
        }

        for (SseEmitter emitter : dead) {
            remove(scheduledTripId, emitter);
        }
    }

    /**
     * Removes a dead emitter from the subscription map.
     * Called by callbacks and during broadcast cleanup.
     */
    private void remove(UUID scheduledTripId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> tripSubs = subscribers.get(scheduledTripId);
        if (tripSubs != null) {
            tripSubs.remove(emitter);
            if (tripSubs.isEmpty()) {
                subscribers.remove(scheduledTripId);
            }
            log.info(
                    "SSE subscriber removed for scheduledTripId={}, remaining={}",
                    scheduledTripId,
                    tripSubs.size());
        }
    }

    /** Exposes subscriber count for testing. */
    int subscriberCount(UUID scheduledTripId) {
        CopyOnWriteArrayList<SseEmitter> tripSubs = subscribers.get(scheduledTripId);
        return tripSubs == null ? 0 : tripSubs.size();
    }
}
