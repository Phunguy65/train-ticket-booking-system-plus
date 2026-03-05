package io.github.phunguy65.ttbs.backend.booking.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking")
public record BookingProperties(String cancelUrl) {}
