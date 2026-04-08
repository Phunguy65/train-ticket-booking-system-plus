package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.payment.application.response.CheckoutSessionResponse;
import io.github.phunguy65.ttbs.backend.payment.application.response.CreateCheckoutResult;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.CreateCheckoutSessionUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByBookingIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.CreateCheckoutRequest;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@DisplayName("PaymentController — createCheckout")
class PaymentControllerTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKING_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final GetPaymentByIdUseCase getPaymentByIdUseCase = mock(GetPaymentByIdUseCase.class);
    private final GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase =
            mock(GetPaymentByBookingIdUseCase.class);
    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase =
            mock(CreateCheckoutSessionUseCase.class);

    private final PaymentController controller = new PaymentController(
            getPaymentByIdUseCase, getPaymentByBookingIdUseCase, createCheckoutSessionUseCase);

    private Authentication auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID.toString(), null);
    }

    @Test
    @DisplayName("returns 201 when checkout session is newly created")
    void createCheckout_returns201_whenCreated() {
        var command = new CreateCheckoutRequest().toCommand(BOOKING_ID, USER_ID);
        var response = new CheckoutSessionResponse(
                PAYMENT_ID, "https://checkout.stripe.com/session/new", PaymentStatus.PENDING);
        when(createCheckoutSessionUseCase.execute(command))
                .thenReturn(Result.success(new CreateCheckoutResult(response, true)));

        var result = controller.createCheckout(BOOKING_ID, auth());

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.CREATED.value());
        @SuppressWarnings("unchecked")
        JsendResponse<CheckoutSessionResponse> body =
                (JsendResponse<CheckoutSessionResponse>) result.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("success");
        assertThat(body.data().paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(body.data().checkoutUrl()).isEqualTo("https://checkout.stripe.com/session/new");
    }

    @Test
    @DisplayName("returns 200 when checkout session already exists (idempotent)")
    void createCheckout_returns200_whenIdempotent() {
        var command = new CreateCheckoutRequest().toCommand(BOOKING_ID, USER_ID);
        var response = new CheckoutSessionResponse(
                PAYMENT_ID, "https://checkout.stripe.com/session/existing", PaymentStatus.PENDING);
        when(createCheckoutSessionUseCase.execute(command))
                .thenReturn(Result.success(new CreateCheckoutResult(response, false)));

        var result = controller.createCheckout(BOOKING_ID, auth());

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    @DisplayName("returns 404 when booking is not found")
    void createCheckout_returns404_whenBookingNotFound() {
        var command = new CreateCheckoutRequest().toCommand(BOOKING_ID, USER_ID);
        when(createCheckoutSessionUseCase.execute(command))
                .thenReturn(Result.failure(new PaymentError.BookingNotFound()));

        var result = controller.createCheckout(BOOKING_ID, auth());

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
        @SuppressWarnings("unchecked")
        JsendResponse<FailData> body = (JsendResponse<FailData>) result.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("fail");
        assertThat(body.data().code()).isEqualTo(ErrorCode.PAYMENT_BOOKING_NOT_FOUND);
    }

    @Test
    @DisplayName("returns 403 when user is forbidden")
    void createCheckout_returns403_whenForbidden() {
        var command = new CreateCheckoutRequest().toCommand(BOOKING_ID, USER_ID);
        when(createCheckoutSessionUseCase.execute(command))
                .thenReturn(Result.failure(new PaymentError.Forbidden()));

        var result = controller.createCheckout(BOOKING_ID, auth());

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        @SuppressWarnings("unchecked")
        JsendResponse<FailData> body = (JsendResponse<FailData>) result.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("fail");
        assertThat(body.data().code()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("returns 409 when booking is in invalid state")
    void createCheckout_returns409_whenInvalidBookingState() {
        var command = new CreateCheckoutRequest().toCommand(BOOKING_ID, USER_ID);
        when(createCheckoutSessionUseCase.execute(command))
                .thenReturn(Result.failure(
                        new PaymentError.InvalidBookingState("Booking is not in HELD status")));

        var result = controller.createCheckout(BOOKING_ID, auth());

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        @SuppressWarnings("unchecked")
        JsendResponse<FailData> body = (JsendResponse<FailData>) result.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("fail");
        assertThat(body.data().code()).isEqualTo(ErrorCode.PAYMENT_BOOKING_INVALID_STATE);
    }

    @Test
    @DisplayName("returns 409 when payment was already processed")
    void createCheckout_returns409_whenAlreadyProcessed() {
        var command = new CreateCheckoutRequest().toCommand(BOOKING_ID, USER_ID);
        when(createCheckoutSessionUseCase.execute(command))
                .thenReturn(Result.failure(new PaymentError.AlreadyProcessed()));

        var result = controller.createCheckout(BOOKING_ID, auth());

        assertThat(result.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        @SuppressWarnings("unchecked")
        JsendResponse<FailData> body = (JsendResponse<FailData>) result.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("fail");
        assertThat(body.data().code()).isEqualTo(ErrorCode.PAYMENT_ALREADY_PROCESSED);
    }
}
