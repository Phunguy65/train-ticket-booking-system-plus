package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.ConfirmSeatHoldUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateSeatHoldUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingUseCase;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/{version}/bookings")
class BookingController {

    private final CreateSeatHoldUseCase createSeatHoldUseCase;
    private final ConfirmSeatHoldUseCase confirmSeatHoldUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final GetBookingUseCase getBookingUseCase;
    private final BookingRequestMapper mapper;

    BookingController(
            CreateSeatHoldUseCase createSeatHoldUseCase,
            ConfirmSeatHoldUseCase confirmSeatHoldUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            GetBookingUseCase getBookingUseCase,
            BookingRequestMapper mapper) {
        this.createSeatHoldUseCase = createSeatHoldUseCase;
        this.confirmSeatHoldUseCase = confirmSeatHoldUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.getBookingUseCase = getBookingUseCase;
        this.mapper = mapper;
    }

    /**
     * POST /api/{version}/bookings/hold
     * Create a multi-seat hold for the specified route and seats.
     */
    @PostMapping(value = "/hold", version = "1.0")
    ResponseEntity<JsendResponse<?>> holdSeats(
            @Valid @RequestBody CreateSeatHoldHttpRequest request) {
        return createSeatHoldUseCase
                .execute(mapper.toCreateHoldCommand(request))
                .fold(
                        dto -> {
                            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .replacePath("/{version}/bookings/{id}")
                                    .buildAndExpand("v1.0", dto.bookingId())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(mapper.toResponse(dto)));
                        },
                        error -> switch (error) {
                            case io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError
                                            .ActiveHoldExists
                                    e ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(JsendResponse.fail(new FailData(
                                                e.message(),
                                                ErrorCode.SEAT_NOT_AVAILABLE,
                                                List.of())));
                            case io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError
                                            .SeatsLocked
                                    e ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(JsendResponse.fail(new FailData(
                                                e.message(),
                                                ErrorCode.SEAT_NOT_AVAILABLE,
                                                List.of())));
                            default ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(JsendResponse.fail(new FailData(
                                                error.message(),
                                                ErrorCode.SEAT_NOT_AVAILABLE,
                                                List.of())));
                        });
    }

    /**
     * POST /api/{version}/bookings/{id}/confirm
     * Confirm a held booking after payment.
     */
    @PostMapping(value = "/{id}/confirm", version = "1.0")
    ResponseEntity<JsendResponse<?>> confirmHold(
            @PathVariable UUID id, @Valid @RequestBody ConfirmSeatHoldHttpRequest request) {
        return confirmSeatHoldUseCase
                .execute(mapper.toConfirmCommand(id, request))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto))),
                        error -> switch (error) {
                            case io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError
                                            .HoldExpired
                                    e ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(JsendResponse.fail(new FailData(
                                                e.message(),
                                                ErrorCode.BOOKING_CANNOT_CONFIRM,
                                                List.of())));
                            case io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError
                                            .InvalidStatusTransition
                                    e ->
                                ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(JsendResponse.fail(new FailData(
                                                e.message(),
                                                ErrorCode.BOOKING_NOT_FOUND,
                                                List.of())));
                            default ->
                                ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(JsendResponse.fail(new FailData(
                                                error.message(),
                                                ErrorCode.BOOKING_CANNOT_CONFIRM,
                                                List.of())));
                        });
    }

    /**
     * DELETE /api/{version}/bookings/{id}
     * Cancel a booking (HELD or CONFIRMED).
     */
    @DeleteMapping(value = "/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> cancelBooking(@PathVariable UUID id) {
        return cancelBookingUseCase
                .execute(mapper.toCancelCommand(id))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto))),
                        error -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(JsendResponse.fail(new FailData(
                                        error.message(), ErrorCode.BOOKING_NOT_FOUND, List.of()))));
    }

    /**
     * GET /api/{version}/bookings/{id}
     * Retrieve a booking by ID.
     */
    @GetMapping(value = "/{id}", version = "1.0")
    ResponseEntity<JsendResponse<?>> getBooking(@PathVariable UUID id) {
        return getBookingUseCase
                .execute(id)
                .<ResponseEntity<JsendResponse<?>>>map(
                        dto -> ResponseEntity.ok(JsendResponse.success(mapper.toResponse(dto))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(new FailData(
                                "Booking not found", ErrorCode.BOOKING_NOT_FOUND, List.of()))));
    }
}
