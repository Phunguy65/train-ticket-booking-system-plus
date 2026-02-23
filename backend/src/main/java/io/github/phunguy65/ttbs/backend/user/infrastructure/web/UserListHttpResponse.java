package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import java.time.Instant;
import java.util.UUID;

/**
 * HTTP response DTO for a single user item in the admin list endpoint.
 *
 * <p>Intentionally omits sensitive fields: {@code passwordHash} and {@code phone}.
 */
record UserListHttpResponse(
        UUID id, String email, String fullName, UserRole role, Instant createdAt) {}
