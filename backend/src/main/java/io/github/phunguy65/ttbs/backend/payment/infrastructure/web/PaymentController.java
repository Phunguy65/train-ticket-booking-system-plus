package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import io.github.phunguy65.ttbs.backend.payment.application.response.CheckoutSessionResponse;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentDetailResponse;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.application.response.UserPaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.CreateCheckoutSessionUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByBookingIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetUserPaymentsUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.CreateCheckoutRequest;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByBookingIdRequest;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByIdRequest;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetUserPaymentsRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Payments")
class PaymentController {

    private final GetPaymentByIdUseCase getPaymentByIdUseCase;
    private final GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase;
    private final GetUserPaymentsUseCase getUserPaymentsUseCase;
    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase;

    PaymentController(
            GetPaymentByIdUseCase getPaymentByIdUseCase,
            GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase,
            GetUserPaymentsUseCase getUserPaymentsUseCase,
            CreateCheckoutSessionUseCase createCheckoutSessionUseCase) {
        this.getPaymentByIdUseCase = getPaymentByIdUseCase;
        this.getPaymentByBookingIdUseCase = getPaymentByBookingIdUseCase;
        this.getUserPaymentsUseCase = getUserPaymentsUseCase;
        this.createCheckoutSessionUseCase = createCheckoutSessionUseCase;
    }

    @Operation(operationId = "getUserPayments", summary = "List payments for a customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paged payment history"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid pagination parameters",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "403",
                description = "Customer cannot access another customer's payments",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(value = UserPaymentResponse.class, kind = SuccessResponseKind.PAGE)
    @GetMapping(value = "/{version}/users/{userId}/payments", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> listByUser(
            @Parameter(description = "Customer identifier that owns the payments") @PathVariable
                    UUID userId,
            @Parameter(hidden = true) Authentication auth,
            @ModelAttribute @Valid GetUserPaymentsRequest request) {
        UUID requestingUserId = UUID.fromString(auth.getName());

        return getUserPaymentsUseCase
                .execute(request.toQuery(userId, requestingUserId))
                .fold(page -> ResponseEntity.ok(JsendResponse.success(page)), this::errorResponse);
    }

    @Operation(operationId = "getPayment", summary = "Get a payment by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment detail"),
        @ApiResponse(
                responseCode = "403",
                description = "Customer cannot access this payment",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Payment not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(PaymentDetailResponse.class)
    @GetMapping(value = "/{version}/payments/{paymentId}", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> getPaymentById(
            @Parameter(description = "Payment identifier") @PathVariable UUID paymentId,
            @Parameter(hidden = true) Authentication auth,
            @ModelAttribute GetPaymentByIdRequest request) {
        UUID userId = UUID.fromString(auth.getName());

        return getPaymentByIdUseCase
                .execute(request.toQuery(paymentId, userId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @Operation(operationId = "getBookingPayment", summary = "Get the payment linked to a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment linked to the booking"),
        @ApiResponse(
                responseCode = "403",
                description = "Customer cannot access this booking payment",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Payment or booking not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(PaymentResponse.class)
    @GetMapping(value = "/{version}/bookings/{bookingId}/payment", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> getPaymentByBookingId(
            @Parameter(description = "Booking identifier") @PathVariable UUID bookingId,
            @Parameter(hidden = true) Authentication auth,
            @ModelAttribute GetPaymentByBookingIdRequest request) {
        UUID userId = UUID.fromString(auth.getName());

        return getPaymentByBookingIdUseCase
                .execute(request.toQuery(bookingId, userId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @Operation(
            operationId = "createCheckoutSession",
            summary = "Create a Stripe checkout session for a booking")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Checkout session created"),
        @ApiResponse(
                responseCode = "200",
                description = "Checkout session already exists (idempotent)"),
        @ApiResponse(
                responseCode = "403",
                description = "Customer cannot create a payment for this booking",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "404",
                description = "Booking not found",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse"))),
        @ApiResponse(
                responseCode = "409",
                description = "Booking is not in a payable state or payment was already processed",
                content =
                        @Content(schema = @Schema(ref = "#/components/schemas/JsendFailResponse")))
    })
    @SecurityRequirement(name = "bearerAuth")
    @SuccessPayload(value = CheckoutSessionResponse.class, responseCode = "201")
    @PostMapping(value = "/{version}/bookings/{bookingId}/checkout", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> createCheckout(
            @Parameter(description = "Booking identifier") @PathVariable UUID bookingId,
            @Parameter(hidden = true) Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        var command = new CreateCheckoutRequest().toCommand(bookingId, userId);

        return createCheckoutSessionUseCase
                .execute(command)
                .fold(
                        result -> {
                            HttpStatus status =
                                    result.created() ? HttpStatus.CREATED : HttpStatus.OK;
                            return ResponseEntity.status(status)
                                    .body(JsendResponse.success(result.response()));
                        },
                        error -> errorResponse(error));
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(PaymentError error) {
        HttpStatus status =
                switch (error) {
                    case PaymentError.PaymentNotFound e -> HttpStatus.NOT_FOUND;
                    case PaymentError.BookingNotFound e -> HttpStatus.NOT_FOUND;
                    case PaymentError.Forbidden e -> HttpStatus.FORBIDDEN;
                    case PaymentError.AlreadyProcessed e -> HttpStatus.CONFLICT;
                    case PaymentError.InvalidBookingState e -> HttpStatus.CONFLICT;
                    case PaymentError.RefundFailed e -> HttpStatus.INTERNAL_SERVER_ERROR;
                };
        ErrorCode code =
                switch (error) {
                    case PaymentError.PaymentNotFound e -> ErrorCode.PAYMENT_NOT_FOUND;
                    case PaymentError.BookingNotFound e -> ErrorCode.PAYMENT_BOOKING_NOT_FOUND;
                    case PaymentError.Forbidden e -> ErrorCode.ACCESS_DENIED;
                    case PaymentError.AlreadyProcessed e -> ErrorCode.PAYMENT_ALREADY_PROCESSED;
                    case PaymentError.InvalidBookingState e ->
                        ErrorCode.PAYMENT_BOOKING_INVALID_STATE;
                    case PaymentError.RefundFailed e -> ErrorCode.PAYMENT_REFUND_FAILED;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
