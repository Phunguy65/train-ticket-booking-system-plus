package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

record CreateUserHttpResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String role,
        Instant createdAt,
        String temporaryPassword) {}
