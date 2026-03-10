package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.payment.application.dto.PaymentDto;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PaymentController {

    private final GetPaymentUseCase getPaymentUseCase;

    PaymentController(GetPaymentUseCase getPaymentUseCase) {
        this.getPaymentUseCase = getPaymentUseCase;
    }

    @GetMapping(value = "/{version}/payments/{bookingId}", version = "1.0")
    @PreAuthorize("isAuthenticated()")
    ResponseEntity<JsendResponse<?>> getPayment(@PathVariable UUID bookingId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        return getPaymentUseCase
                .execute(BookingId.of(bookingId), UserId.of(userId))
                .fold(
                        dto -> ResponseEntity.ok(JsendResponse.success(toResponse(dto))),
                        error -> errorResponse(error));
    }

    private PaymentHttpResponse toResponse(PaymentDto dto) {
        return new PaymentHttpResponse(
                dto.paymentId(),
                dto.bookingId(),
                dto.status(),
                dto.checkoutUrl(),
                dto.amount(),
                dto.currency());
    }

    private ResponseEntity<JsendResponse<?>> errorResponse(PaymentError error) {
        HttpStatus status =
                switch (error) {
                    case PaymentError.PaymentNotFound e -> HttpStatus.NOT_FOUND;
                    case PaymentError.AlreadyProcessed e -> HttpStatus.CONFLICT;
                    case PaymentError.RefundFailed e -> HttpStatus.INTERNAL_SERVER_ERROR;
                };
        ErrorCode code =
                switch (error) {
                    case PaymentError.PaymentNotFound e -> ErrorCode.PAYMENT_NOT_FOUND;
                    case PaymentError.AlreadyProcessed e -> ErrorCode.PAYMENT_ALREADY_PROCESSED;
                    case PaymentError.RefundFailed e -> ErrorCode.PAYMENT_REFUND_FAILED;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }
}
