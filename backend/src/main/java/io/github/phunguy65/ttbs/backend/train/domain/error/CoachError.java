package io.github.phunguy65.ttbs.backend.train.domain.error;

import java.util.List;
import java.util.UUID;

/**
 * Typed business errors for the Coach domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface CoachError {

    record CoachNotFound() implements CoachError {
        @Override
        public String message() {
            return "Coach not found";
        }
    }

    record CarNumberAlreadyExists(int carNumber) implements CoachError {
        @Override
        public String message() {
            return "A coach with car number '" + carNumber + "' already exists on this train";
        }
    }

    record TrainNotFound() implements CoachError {
        @Override
        public String message() {
            return "Train not found";
        }
    }

    record CoachInUse(List<UUID> conflictingIds) implements CoachError {
        @Override
        public String message() {
            return "One or more coaches have active seats and cannot be deleted";
        }
    }

    record CarNumbersAlreadyExist(List<Integer> conflictingCarNumbers) implements CoachError {
        @Override
        public String message() {
            return "One or more car numbers already exist on this train: " + conflictingCarNumbers;
        }
    }

    record DuplicateCarNumbersInRequest(List<Integer> duplicates) implements CoachError {
        @Override
        public String message() {
            return "Duplicate car numbers in request: " + duplicates;
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
