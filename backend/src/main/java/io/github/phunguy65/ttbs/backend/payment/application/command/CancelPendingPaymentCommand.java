package io.github.phunguy65.ttbs.backend.payment.application.command;

public record CancelPendingPaymentCommand(String checkoutSessionId) {}
