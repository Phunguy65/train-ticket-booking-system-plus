package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.CreateBookingRequest;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class BookingRequestMapper {

    CreateBookingCommand toCommand(CreateBookingRequest request, UUID userId) {
        return new CreateBookingCommand(
                userId,
                request.routeId(),
                request.seatIds().stream().map(SeatId::of).toList(),
                request.passengerName(),
                request.passengerEmail(),
                request.passengerPhone(),
                request.idempotencyKey());
    }

    BookingHttpResponse toResponse(BookingResponse dto) {
        return new BookingHttpResponse(
                dto.id(),
                dto.userId(),
                dto.routeId(),
                dto.passengerName(),
                dto.passengerEmail(),
                dto.passengerPhone(),
                dto.totalPrice(),
                dto.currency(),
                dto.status(),
                dto.paymentDeadline(),
                dto.createdAt());
    }
}
