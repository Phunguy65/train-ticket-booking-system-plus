package io.github.phunguy65.ttbs.backend.user.application.dto;

public record LoginResultDto(String accessToken, String refreshToken, UserDto user) {}
