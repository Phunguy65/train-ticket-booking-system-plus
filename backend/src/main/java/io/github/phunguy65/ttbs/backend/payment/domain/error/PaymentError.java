package io.github.phunguy65.ttbs.backend.payment.domain.error;

/**
 * Typed business errors for the payment domain.
 *
 * <p>Returned via {@link io.github.phunguy65.ttbs.backend.shared.domain.Result} — never thrown.
 */
public sealed interface PaymentError {

    record PaymentNotFound() implements PaymentError {
        @Override
        public String message() {
            return "Payment not found";
        }
    }

    record AlreadyProcessed() implements PaymentError {
        @Override
        public String message() {
            return "Payment event already processed";
        }
    }

    record RefundFailed(String reason) implements PaymentError {
        @Override
        public String message() {
            return "Refund failed: " + reason;
        }
    }

    /** Human-readable description suitable for a JSend {@code fail} data payload. */
    String message();
}
