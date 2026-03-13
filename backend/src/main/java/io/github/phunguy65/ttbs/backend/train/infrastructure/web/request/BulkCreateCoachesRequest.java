package io.github.phunguy65.ttbs.backend.train.infrastructure.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

record BulkCreateCoachesRequest(
        @NotEmpty(message = "At least one coach must be provided") @Size(max = 100, message = "Bulk create is limited to 100 coaches per request") List<@Valid CoachItemRequest> coaches) {

    record CoachItemRequest(
            @Positive(message = "Car number must be a positive number") int carNumber,

            @Positive(message = "Total seats must be a positive number") int totalSeats) {}
}
