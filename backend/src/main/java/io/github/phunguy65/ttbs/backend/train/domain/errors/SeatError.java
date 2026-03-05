package io.github.phunguy65.ttbs.backend.train.domain.errors;

import java.util.List;
import java.util.UUID;

/**
 * Typed business errors for the Seat domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface SeatError {

    record SeatNotFound() implements SeatError {
        @Override
        public String message() {
            return "Seat not found";
        }
    }

    record SeatNumberAlreadyExists(String seatNumber) implements SeatError {
        @Override
        public String message() {
            return "A seat with number '" + seatNumber + "' already exists on this train";
        }
    }

    record TrainNotFound() implements SeatError {
        @Override
        public String message() {
            return "Train not found";
        }
    }

    record SeatInUse(List<UUID> conflictingIds) implements SeatError {
        @Override
        public String message() {
            return "One or more seats have active holds or bookings and cannot be deleted";
        }
    }

    record CoachNotFound() implements SeatError {
        @Override
        public String message() {
            return "Coach not found";
        }
    }

    record SeatNumbersAlreadyExist(List<String> conflictingNumbers) implements SeatError {
        @Override
        public String message() {
            return "One or more seat numbers already exist on this coach: " + conflictingNumbers;
        }
    }

    record DuplicateSeatNumbersInRequest(List<String> duplicates) implements SeatError {
        @Override
        public String message() {
            return "Duplicate seat numbers in request: " + duplicates;
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
