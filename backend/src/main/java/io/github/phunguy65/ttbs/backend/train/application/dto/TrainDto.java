package io.github.phunguy65.ttbs.backend.train.application.dto;

import java.time.Instant;
import java.util.UUID;

public record TrainDto(
        UUID id, String trainNumber, String name, int totalSeats, Instant createdAt) {}
