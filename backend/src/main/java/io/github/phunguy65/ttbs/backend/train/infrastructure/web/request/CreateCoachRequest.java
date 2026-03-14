package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.command.CreateCoachCommand;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CreateCoachRequest(
        @Positive(message = "Car number must be a positive number") int carNumber,

        @Positive(message = "Total seats must be a positive number") int totalSeats) {

    public CreateCoachCommand toCommand(UUID trainId) {
        return new CreateCoachCommand(trainId, carNumber, totalSeats);
    }
}
