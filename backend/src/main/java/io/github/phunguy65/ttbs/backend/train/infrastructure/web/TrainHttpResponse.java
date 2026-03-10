package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

record TrainHttpResponse(
        UUID id, String trainNumber, String name, int totalSeats, Instant createdAt) {}
