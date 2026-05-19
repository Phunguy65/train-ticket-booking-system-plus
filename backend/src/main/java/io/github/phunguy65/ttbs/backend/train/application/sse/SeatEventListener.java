package io.github.phunguy65.ttbs.backend.train.application.sse;

import io.github.phunguy65.ttbs.backend.shared.domain.event.SeatStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link SeatStatusChangedEvent} and delegates broadcasting to
 * {@link SeatEventBroadcaster}.
 *
 * <p>Uses {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to ensure SSE events
 * are only sent after the originating transaction has successfully committed.
 */
@Component
public class SeatEventListener {

    private static final Logger log = LoggerFactory.getLogger(SeatEventListener.class);

    private final SeatEventBroadcaster broadcaster;

    public SeatEventListener(SeatEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Handles seat status change events that were published inside a successfully committed
     * transaction.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeatStatusChanged(SeatStatusChangedEvent event) {
        log.debug(
                "Received SeatStatusChangedEvent for scheduledTripId={}, seatCount={}",
                event.scheduledTripId(),
                event.seats().size());
        broadcaster.broadcast(event.scheduledTripId(), event);
    }
}
