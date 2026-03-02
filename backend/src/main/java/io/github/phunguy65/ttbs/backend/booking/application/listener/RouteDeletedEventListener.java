package io.github.phunguy65.ttbs.backend.booking.application.listener;

import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.train.domain.event.RouteDeleted;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link RouteDeleted} domain events and cascades the soft-delete
 * to all active bookings on the deleted route.
 *
 * <p>Uses {@link TransactionPhase#BEFORE_COMMIT} so the booking update runs
 * inside the same transaction as the route deletion — if it fails, both roll back atomically.
 */
@Component
public class RouteDeletedEventListener {

    private final BookingRepository bookingRepository;

    public RouteDeletedEventListener(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onRouteDeleted(RouteDeleted event) {
        bookingRepository.softDeleteByRouteId(event.routeId(), event.occurredAt());
    }
}
