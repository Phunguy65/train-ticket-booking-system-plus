package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.application.port.StaleHoldQueryPort;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class StaleHoldQueryAdapter implements StaleHoldQueryPort {

    private final BookingRepository bookingRepository;

    StaleHoldQueryAdapter(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<StaleHoldView> findStaleHoldsWithCheckoutSession(Instant threshold) {
        return bookingRepository.findStaleHoldsWithCheckoutSession(threshold).stream()
                .map(b -> new StaleHoldView(b.getId().value(), b.getCheckoutSessionId()))
                .toList();
    }
}
