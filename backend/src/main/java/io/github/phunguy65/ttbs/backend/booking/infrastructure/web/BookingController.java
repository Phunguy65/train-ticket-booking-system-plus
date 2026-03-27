package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.CreateBookingRequest;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;

    BookingController(
            CreateBookingUseCase createBookingUseCase, CancelBookingUseCase cancelBookingUseCase) {
        this.createBookingUseCase = createBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
    }

    @PostMapping(value = "/{version}/bookings", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> create(@Valid @RequestBody CreateBookingRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        return createBookingUseCase
                .execute(request.toCommand(userId))
                .fold(
                        dto -> {
                            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .path("/{id}")
                                    .buildAndExpand(dto.id())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(dto));
                        },
                        error -> errorResponse(error));
    }

    @PostMapping(value = "/{version}/bookings/{id}/cancel", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> cancel(@PathVariable UUID id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        return cancelBookingUseCase
                .execute(new CancelBookingCommand(id, userId))
                .fold(
                        v -> ResponseEntity.ok(JsendResponse.success()),
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(BookingError error) {
        HttpStatus status =
                switch (error) {
                    case BookingError.BookingNotFound e -> HttpStatus.NOT_FOUND;
                    case BookingError.ScheduledTripNotFound e -> HttpStatus.NOT_FOUND;
                    case BookingError.SeatNotAvailable e -> HttpStatus.CONFLICT;
                    case BookingError.ActiveHoldExists e -> HttpStatus.CONFLICT;
                    case BookingError.InvalidStatusTransition e -> HttpStatus.CONFLICT;
                    case BookingError.Forbidden e -> HttpStatus.FORBIDDEN;
                };
        ErrorCode code =
                switch (error) {
                    case BookingError.BookingNotFound e -> ErrorCode.BOOKING_NOT_FOUND;
                    case BookingError.ScheduledTripNotFound e -> ErrorCode.SCHEDULED_TRIP_NOT_FOUND;
                    case BookingError.SeatNotAvailable e -> ErrorCode.SEAT_NOT_AVAILABLE;
                    case BookingError.ActiveHoldExists e -> ErrorCode.BOOKING_CANNOT_CONFIRM;
                    case BookingError.InvalidStatusTransition e ->
                        ErrorCode.BOOKING_ALREADY_CANCELLED;
                    case BookingError.Forbidden e -> ErrorCode.ACCESS_DENIED;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
