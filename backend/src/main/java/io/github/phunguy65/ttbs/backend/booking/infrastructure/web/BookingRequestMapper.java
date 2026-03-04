package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.command.ConfirmSeatHoldCommand;
import io.github.phunguy65.ttbs.backend.booking.application.command.CreateSeatHoldCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import org.springframework.stereotype.Component;

@Component
class BookingRequestMapper {

    CreateSeatHoldCommand toCreateHoldCommand(CreateSeatHoldHttpRequest request) {
        return new CreateSeatHoldCommand(
                request.userId(),
                request.routeId(),
                request.seatIds(),
                request.idempotencyKey(),
                request.passengerName(),
                request.passengerEmail(),
                request.passengerPhone());
    }

    ConfirmSeatHoldCommand toConfirmCommand(java.util.UUID bookingId) {
        return new ConfirmSeatHoldCommand(bookingId);
    }

    CancelBookingCommand toCancelCommand(java.util.UUID bookingId) {
        return new CancelBookingCommand(bookingId);
    }

    BookingHttpResponse toResponse(HoldDto dto) {
        java.util.List<BookingHttpResponse.BookedSeatResponse> seats = dto.seats().stream()
                .map(s -> new BookingHttpResponse.BookedSeatResponse(s.seatId(), s.unitPrice()))
                .toList();
        java.time.Instant expiresAt = dto.expiresAt();
        return new BookingHttpResponse(
                dto.bookingId(),
                null,
                dto.routeId(),
                dto.status(),
                seats,
                dto.totalPrice(),
                dto.currency(),
                null,
                expiresAt,
                dto.checkoutUrl(),
                dto.checkoutSessionId());
    }
}
