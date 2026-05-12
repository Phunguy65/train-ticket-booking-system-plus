package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.application.command.CancelBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingDetailUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetUserBookingsUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.CreateBookingRequest;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetBookingDetailRequest;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetUserBookingsRequest;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessPayload;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessResponseKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@Tag(name = "Bookings")
class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final CancelBookingUseCase cancelBookingUseCase;
    private final GetBookingDetailUseCase getBookingDetailUseCase;
    private final GetUserBookingsUseCase getUserBookingsUseCase;

    BookingController(
            CreateBookingUseCase createBookingUseCase,
            CancelBookingUseCase cancelBookingUseCase,
            GetBookingDetailUseCase getBookingDetailUseCase,
            GetUserBookingsUseCase getUserBookingsUseCase) {
        this.createBookingUseCase = createBookingUseCase;
        this.cancelBookingUseCase = cancelBookingUseCase;
        this.getBookingDetailUseCase = getBookingDetailUseCase;
        this.getUserBookingsUseCase = getUserBookingsUseCase;
    }

    @Operation(operationId = "getUserBookings", summary = "List bookings for a customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged booking history"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "403",
                description = "Customer cannot access another customer's bookings",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(value = UserBookingResponse.class, kind = SuccessResponseKind.PAGE)
    @org.springframework.web.bind.annotation.GetMapping(
            value = "/{version}/users/{userId}/bookings",
            version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> listByUser(
            @Parameter(description = "Customer identifier that owns the bookings") @PathVariable
                    UUID userId,
            @Parameter(hidden = true) Authentication auth,
            @ParameterObject @Valid GetUserBookingsRequest request) {
        UUID requestingUserId = UUID.fromString(auth.getName());

        return getUserBookingsUseCase
                .execute(request.toQuery(userId, requestingUserId))
                .fold(page -> ResponseEntity.ok(JsendResponse.success(page)), this::errorResponse);
    }

    @Operation(operationId = "getBooking", summary = "Get a booking by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking detail"),
        @ApiResponse(
                responseCode = "403",
                description = "Customer cannot access this booking",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Booking not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(BookingDetailResponse.class)
    @org.springframework.web.bind.annotation.GetMapping(
            value = "/{version}/bookings/{id}",
            version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> getById(
            @Parameter(description = "Booking identifier") @PathVariable UUID id,
            @Parameter(hidden = true) Authentication auth,
            @ParameterObject GetBookingDetailRequest request) {
        UUID userId = UUID.fromString(auth.getName());

        return getBookingDetailUseCase
                .execute(request.toQuery(id, userId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @Operation(operationId = "createBooking", summary = "Create a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid booking payload",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Scheduled trip or customer not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "Booking cannot be created because seats are unavailable or already held",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(value = BookingResponse.class, responseCode = "201")
    @PostMapping(value = "/{version}/bookings", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> create(
            @Valid @RequestBody CreateBookingRequest request,
            @Parameter(hidden = true) Authentication auth) {
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

    @Operation(operationId = "cancelBooking", summary = "Cancel an existing booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled"),
        @ApiResponse(
                responseCode = "403",
                description = "Customer cannot cancel this booking",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Booking not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "409",
                description = "Booking cannot transition to cancelled state",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload
    @PostMapping(value = "/{version}/bookings/{id}/cancel", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> cancel(
            @Parameter(description = "Booking identifier") @PathVariable UUID id,
            @Parameter(hidden = true) Authentication auth) {
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
                    case BookingError.UserNotFound e -> HttpStatus.NOT_FOUND;
                    case BookingError.SeatNotAvailable e -> HttpStatus.CONFLICT;
                    case BookingError.ActiveHoldExists e -> HttpStatus.CONFLICT;
                    case BookingError.InvalidStatusTransition e -> HttpStatus.CONFLICT;
                    case BookingError.Forbidden e -> HttpStatus.FORBIDDEN;
                    case BookingError.TooManySeats e -> HttpStatus.BAD_REQUEST;
                    case BookingError.PassengerSeatMismatch e -> HttpStatus.BAD_REQUEST;
                    case BookingError.DuplicatePassengerIdDocument e -> HttpStatus.BAD_REQUEST;
                    case BookingError.InvalidPassengerSeatAssignment e -> HttpStatus.BAD_REQUEST;
                    case BookingError.DuplicatePassengerSeatAssignment e -> HttpStatus.BAD_REQUEST;
                };
        ErrorCode code =
                switch (error) {
                    case BookingError.BookingNotFound e -> ErrorCode.BOOKING_NOT_FOUND;
                    case BookingError.ScheduledTripNotFound e -> ErrorCode.SCHEDULED_TRIP_NOT_FOUND;
                    case BookingError.UserNotFound e -> ErrorCode.USER_NOT_FOUND;
                    case BookingError.SeatNotAvailable e -> ErrorCode.SEAT_NOT_AVAILABLE;
                    case BookingError.ActiveHoldExists e -> ErrorCode.BOOKING_CANNOT_CONFIRM;
                    case BookingError.InvalidStatusTransition e ->
                        ErrorCode.BOOKING_ALREADY_CANCELLED;
                    case BookingError.Forbidden e -> ErrorCode.ACCESS_DENIED;
                    case BookingError.TooManySeats e -> ErrorCode.VALIDATION_ERROR;
                    case BookingError.PassengerSeatMismatch e -> ErrorCode.VALIDATION_ERROR;
                    case BookingError.DuplicatePassengerIdDocument e -> ErrorCode.VALIDATION_ERROR;
                    case BookingError.InvalidPassengerSeatAssignment e ->
                        ErrorCode.VALIDATION_ERROR;
                    case BookingError.DuplicatePassengerSeatAssignment e ->
                        ErrorCode.VALIDATION_ERROR;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
