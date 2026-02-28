package io.github.phunguy65.ttbs.backend.train.infrastructure.web;

import jakarta.validation.constraints.Positive;

record CreateCoachHttpRequest(
        @Positive(message = "Car number must be a positive number") int carNumber,

        @Positive(message = "Total seats must be a positive number") int totalSeats) {}
