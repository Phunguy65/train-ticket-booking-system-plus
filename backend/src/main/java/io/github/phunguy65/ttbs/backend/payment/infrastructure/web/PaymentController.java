package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentResponse;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByBookingIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByBookingIdRequest;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByIdRequest;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.SuccessPayload;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Payments")
class PaymentController {

    private final GetPaymentByIdUseCase getPaymentByIdUseCase;
    private final GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase;

    PaymentController(
            GetPaymentByIdUseCase getPaymentByIdUseCase,
            GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase) {
        this.getPaymentByIdUseCase = getPaymentByIdUseCase;
        this.getPaymentByBookingIdUseCase = getPaymentByBookingIdUseCase;
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
    @SuccessPayload(PaymentResponse.class)
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

    private ResponseEntity<JsendResponse<?>> errorResponse(PaymentError error) {
        HttpStatus status =
                switch (error) {
                    case PaymentError.PaymentNotFound e -> HttpStatus.NOT_FOUND;
                    case PaymentError.Forbidden e -> HttpStatus.FORBIDDEN;
                    case PaymentError.AlreadyProcessed e -> HttpStatus.CONFLICT;
                    case PaymentError.RefundFailed e -> HttpStatus.INTERNAL_SERVER_ERROR;
                };
        ErrorCode code =
                switch (error) {
                    case PaymentError.PaymentNotFound e -> ErrorCode.PAYMENT_NOT_FOUND;
                    case PaymentError.Forbidden e -> ErrorCode.ACCESS_DENIED;
                    case PaymentError.AlreadyProcessed e -> ErrorCode.PAYMENT_ALREADY_PROCESSED;
                    case PaymentError.RefundFailed e -> ErrorCode.PAYMENT_REFUND_FAILED;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
