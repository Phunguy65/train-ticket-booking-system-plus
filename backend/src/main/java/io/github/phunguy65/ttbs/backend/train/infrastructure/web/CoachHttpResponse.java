package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

record CoachHttpResponse(UUID id, UUID trainId, int carNumber, int totalSeats, Instant createdAt) {}
