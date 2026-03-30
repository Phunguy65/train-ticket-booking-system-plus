package io.github.phunguy65.ttbs.backend.payment.application.command;

public record HandlePaymentFailedByPaymentIntentCommand(
        String stripePaymentIntentId, String errorMessage, String stripeEventId) {}
