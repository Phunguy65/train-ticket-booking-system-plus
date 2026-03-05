package io.github.phunguy65.ttbs.backend.booking.application.port;

public interface CheckoutSessionPort {

    CheckoutSessionDto createSession(CheckoutSessionDto.CreateCommand command);
}
