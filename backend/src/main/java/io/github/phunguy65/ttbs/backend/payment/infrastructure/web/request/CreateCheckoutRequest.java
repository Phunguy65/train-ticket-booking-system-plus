package io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.command.CreateCheckoutSessionCommand;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(
        description = "No request body is required — the booking identifier comes from the URL path"
                + " and the user identity from the authentication token.")
public record CreateCheckoutRequest() {

    public CreateCheckoutSessionCommand toCommand(UUID bookingId, UUID userId) {
        return new CreateCheckoutSessionCommand(BookingId.of(bookingId), UserId.of(userId));
    }
}
