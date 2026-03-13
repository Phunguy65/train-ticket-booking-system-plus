package io.github.phunguy65.ttbs.backend.user.application.response;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id, String email, String fullName, String phone, UserRole role, Instant createdAt) {}
