package io.github.phunguy65.ttbs.backend.train.application.response;

import java.time.Instant;
import java.util.UUID;

public record CoachResponse(
        UUID id, UUID trainId, int carNumber, int totalSeats, Instant createdAt) {}
