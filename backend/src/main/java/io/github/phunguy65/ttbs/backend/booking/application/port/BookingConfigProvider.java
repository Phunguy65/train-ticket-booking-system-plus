package io.github.phunguy65.ttbs.backend.booking.application.port;

/**
 * Port for accessing booking configuration from the application layer.
 */
public interface BookingConfigProvider {

    /**
     * Maximum number of seats allowed per booking.
     */
    int getMaxSeatsPerBooking();
}
