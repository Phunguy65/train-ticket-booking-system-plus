package io.github.phunguy65.ttbs.backend.booking.domain.errors;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import java.util.List;
import java.util.UUID;

/**
 * Typed business errors that can occur within the Booking aggregate.
 *
 * <p>These are <em>not</em> exceptions – they are plain data values returned via
 * {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} so that callers can
 * pattern-match exhaustively without relying on exception control-flow.
 */
public sealed interface BookingError {

    /**
     * Attempted to confirm a booking that is not in {@code HELD} status.
     *
     * @param currentStatus the actual status of the booking at the time of the attempt
     */
    record CannotConfirm(BookingStatus currentStatus) implements BookingError {
        @Override
        public String message() {
            return "Cannot confirm booking in status " + currentStatus
                    + ". Only HELD bookings can be confirmed.";
        }
    }

    /**
     * Attempted to cancel a booking that is already {@code CANCELLED}.
     */
    record AlreadyCancelled() implements BookingError {
        @Override
        public String message() {
            return "Booking is already CANCELLED.";
        }
    }

    /**
     * Attempted to create a booking for a seat that is not available on the route.
     * Returned when the seat availability check fails (seat is already BOOKED or does not exist).
     */
    record SeatNotAvailable() implements BookingError {
        @Override
        public String message() {
            return "The requested seat is not available for this route.";
        }
    }

    /**
     * One or more of the requested seats are not available.
     *
     * @param seatIds the list of unavailable seat IDs
     */
    record SeatsNotAvailable(List<UUID> seatIds) implements BookingError {
        @Override
        public String message() {
            return "The following seats are not available: " + seatIds;
        }
    }

    /**
     * Could not acquire pessimistic locks on the requested seats within the timeout period.
     * The client should retry.
     */
    record SeatsLocked() implements BookingError {
        @Override
        public String message() {
            return "The requested seats are temporarily locked by another transaction. Please retry.";
        }
    }

    /**
     * A user already has an active hold on this route.
     */
    record ActiveHoldExists() implements BookingError {
        @Override
        public String message() {
            return "An active hold already exists for this user and route. "
                    + "Cancel or confirm the existing hold before creating a new one.";
        }
    }

    /**
     * The hold has expired — the payment deadline has passed.
     */
    record HoldExpired() implements BookingError {
        @Override
        public String message() {
            return "The seat hold has expired. Please create a new hold.";
        }
    }

    /**
     * The booking is in a status that does not allow the requested transition.
     *
     * @param current the current status of the booking
     */
    record InvalidStatusTransition(BookingStatus current) implements BookingError {
        @Override
        public String message() {
            return "Invalid status transition from " + current + ".";
        }
    }

    /** Human-readable description of the error, suitable for a JSend {@code fail} data payload. */
    String message();
}
