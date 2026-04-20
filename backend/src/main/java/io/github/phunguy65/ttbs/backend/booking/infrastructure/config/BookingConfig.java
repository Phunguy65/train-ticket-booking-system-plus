package io.github.phunguy65.ttbs.backend.booking.infrastructure.config;

import io.github.phunguy65.ttbs.backend.booking.application.port.BookingConfigProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the booking domain.
 */
@Component
public class BookingConfig implements BookingConfigProvider {

    private final int maxSeatsPerBooking;

    public BookingConfig(@Value("${booking.max-seats-per-booking:5}") int maxSeatsPerBooking) {
        this.maxSeatsPerBooking = maxSeatsPerBooking;
    }

    /**
     * Maximum number of seats allowed per booking.
     */
    @Override
    public int getMaxSeatsPerBooking() {
        return maxSeatsPerBooking;
    }
}
