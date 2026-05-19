package io.github.phunguy65.ttbs.backend.payment.application.command;

public record HandlePaymentSuccessCommand(
        String checkoutSessionId, String stripePaymentIntentId, String stripeEventId) {}
