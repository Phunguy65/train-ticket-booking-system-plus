package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import jakarta.validation.constraints.Positive;

record CreateCoachRequest(
        @Positive(message = "Car number must be a positive number") int carNumber,

        @Positive(message = "Total seats must be a positive number") int totalSeats) {}
