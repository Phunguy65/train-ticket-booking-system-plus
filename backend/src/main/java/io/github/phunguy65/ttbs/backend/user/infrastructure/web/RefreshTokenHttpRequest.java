package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

record RefreshTokenHttpRequest(
        @NotBlank(message = "Refresh token is required") String refreshToken) {}
