package io.github.phunguy65.ttbs.backend.user.application.dto;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id, String email, String fullName, String phone, UserRole role, Instant createdAt) {}
