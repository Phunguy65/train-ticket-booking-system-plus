package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingUseCase;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
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

    private final CreateBookingUseCase createBookingUseCase;
    private final GetBookingUseCase getBookingUseCase;
    private final BookingRequestMapper mapper;

    BookingController(
            CreateBookingUseCase createBookingUseCase,
            GetBookingUseCase getBookingUseCase,
            BookingRequestMapper mapper) {
        this.createBookingUseCase = createBookingUseCase;
        this.getBookingUseCase = getBookingUseCase;
        this.mapper = mapper;
    }

    @PostMapping(version = "1.0")
    ResponseEntity<JsendResponse<?>> createBooking(@RequestBody CreateBookingHttpRequest request) {
        return createBookingUseCase
                .execute(mapper.toCommand(request))
                .fold(
                        dto -> {
                            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                    .path("/{id}")
                                    .buildAndExpand(dto.id())
                                    .toUri();
                            return ResponseEntity.created(location)
                                    .body(JsendResponse.success(mapper.toResponse(dto)));
                        },
                        error -> ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(JsendResponse.fail(new FailData(
                                        error.message(),
                                        ErrorCode.SEAT_NOT_AVAILABLE,
                                        List.of()))));
    }

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
