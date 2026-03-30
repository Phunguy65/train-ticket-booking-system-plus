package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByBookingIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByBookingIdRequest;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByIdRequest;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
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
class PaymentController {

    private final GetPaymentByIdUseCase getPaymentByIdUseCase;
    private final GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase;

    PaymentController(
            GetPaymentByIdUseCase getPaymentByIdUseCase,
            GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase) {
        this.getPaymentByIdUseCase = getPaymentByIdUseCase;
        this.getPaymentByBookingIdUseCase = getPaymentByBookingIdUseCase;
    }

    @GetMapping(value = "/{version}/payments/{paymentId}", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> getPaymentById(
            @PathVariable UUID paymentId,
            Authentication auth,
            @ModelAttribute GetPaymentByIdRequest request) {
        UUID userId = UUID.fromString(auth.getName());

        return getPaymentByIdUseCase
                .execute(request.toQuery(paymentId, userId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(dto)),
                        error -> errorResponse(error));
    }

    @GetMapping(value = "/{version}/bookings/{bookingId}/payment", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> getPaymentByBookingId(
            @PathVariable UUID bookingId,
            Authentication auth,
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
