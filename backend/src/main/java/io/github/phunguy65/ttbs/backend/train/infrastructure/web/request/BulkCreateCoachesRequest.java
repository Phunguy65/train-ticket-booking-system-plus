package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.train.application.command.BulkCreateCoachesCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record BulkCreateCoachesRequest(
        @NotEmpty(message = "At least one coach must be provided") @Size(max = 100, message = "Bulk create is limited to 100 coaches per request") List<@Valid CoachItemRequest> coaches) {

    record CoachItemRequest(
            @Positive(message = "Car number must be a positive number") int carNumber,

            @Positive(message = "Total seats must be a positive number") int totalSeats) {}

    public BulkCreateCoachesCommand toCommand(UUID trainId) {
        List<BulkCreateCoachesCommand.CoachItem> items = coaches.stream()
                .map(c -> new BulkCreateCoachesCommand.CoachItem(c.carNumber(), c.totalSeats()))
                .toList();
        return new BulkCreateCoachesCommand(trainId, items);
    }
}
