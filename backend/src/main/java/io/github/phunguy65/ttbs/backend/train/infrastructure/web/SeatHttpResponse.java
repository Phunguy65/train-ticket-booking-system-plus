package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

record SeatHttpResponse(UUID id, UUID trainId, String seatNumber, Instant createdAt) {}
