package io.github.phunguy65.ttbs.backend.user.application.response;

public record LoginResultResponse(String accessToken, String refreshToken, UserResponse user) {}
