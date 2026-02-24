package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import org.springframework.stereotype.Component;

@Component
class BookingRequestMapper {

    CreateBookingCommand toCommand(CreateBookingHttpRequest request) {
        return new CreateBookingCommand(
                request.userId(), request.routeId(), request.seatId(), request.idempotencyKey());
    }

    BookingHttpResponse toResponse(BookingDto dto) {
        return new BookingHttpResponse(
                dto.id(),
                dto.userId(),
                dto.routeId(),
                dto.seatId(),
                dto.status(),
                dto.totalPrice(),
                dto.currency(),
                dto.idempotencyKey());
    }
}
