package io.github.phunguy65.ttbs.backend.train.domain.projection;

import java.time.Instant;
import java.util.UUID;

public record TrainSummary(
        UUID id, String trainNumber, String name, int totalSeats, Instant createdAt) {}
