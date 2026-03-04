package io.github.phunguy65.ttbs.backend.payment.domain.errors;

public sealed interface PaymentError {

    record AlreadyProcessed(String currentStatus) implements PaymentError {
        @Override
        public String message() {
            return "Payment has already been processed with status: " + currentStatus;
        }
    }

    String message();
}
