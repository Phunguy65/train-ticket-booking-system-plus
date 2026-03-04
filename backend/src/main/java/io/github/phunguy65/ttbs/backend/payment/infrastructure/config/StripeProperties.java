package io.github.phunguy65.ttbs.backend.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String apiKey, String webhookSecret, String successUrl, String cancelUrl) {}
