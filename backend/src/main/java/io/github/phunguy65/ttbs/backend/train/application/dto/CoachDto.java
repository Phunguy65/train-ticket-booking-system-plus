package io.github.phunguy65.ttbs.backend.train.application.dto;

import java.time.Instant;
import java.util.UUID;

public record CoachDto(UUID id, UUID trainId, int carNumber, int totalSeats, Instant createdAt) {}
