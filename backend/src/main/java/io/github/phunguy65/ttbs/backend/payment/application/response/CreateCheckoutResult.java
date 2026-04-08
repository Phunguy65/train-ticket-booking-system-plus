package io.github.phunguy65.ttbs.backend.payment.application.response;

/**
 * Internal wrapper returned by {@link
 * io.github.phunguy65.ttbs.backend.payment.application.usecase.CreateCheckoutSessionUseCase} so
 * the controller can distinguish HTTP 201 (newly created) from 200 (idempotent hit).
 *
 * <p>This type is <b>not</b> serialised to JSON — the controller extracts {@link #response()} for
 * the response body and uses {@link #created()} to choose the status code.
 */
public record CreateCheckoutResult(CheckoutSessionResponse response, boolean created) {}
