package io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID routeId,
        @NotEmpty List<UUID> seatIds,
        @NotBlank String passengerName,
        @Email @NotBlank String passengerEmail,
        String passengerPhone,
        @NotBlank String idempotencyKey) {

    public CreateBookingCommand toCommand(UUID userId) {
        return new CreateBookingCommand(
                userId,
                routeId,
                seatIds.stream().map(SeatId::of).toList(),
                passengerName,
                passengerEmail,
                passengerPhone,
                idempotencyKey);
    }
}
