package io.github.phunguy65.ttbs.backend.user.application.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id, String email, String fullName, String phone, String role, Instant createdAt) {}
