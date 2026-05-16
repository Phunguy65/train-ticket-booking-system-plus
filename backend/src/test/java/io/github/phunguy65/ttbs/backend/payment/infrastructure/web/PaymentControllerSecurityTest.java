package io.github.phunguy65.ttbs.backend.payment.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.phunguy65.ttbs.backend.payment.application.usecase.CreateCheckoutSessionUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByBookingIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetPaymentByIdUseCase;
import io.github.phunguy65.ttbs.backend.payment.application.usecase.GetUserPaymentsUseCase;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByBookingIdRequest;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetPaymentByIdRequest;
import io.github.phunguy65.ttbs.backend.payment.infrastructure.web.request.GetUserPaymentsRequest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@DisplayName("PaymentController security")
class PaymentControllerSecurityTest {

    private final GetPaymentByIdUseCase getPaymentByIdUseCase = mock(GetPaymentByIdUseCase.class);
    private final GetPaymentByBookingIdUseCase getPaymentByBookingIdUseCase =
            mock(GetPaymentByBookingIdUseCase.class);
    private final GetUserPaymentsUseCase getUserPaymentsUseCase =
            mock(GetUserPaymentsUseCase.class);
    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase =
            mock(CreateCheckoutSessionUseCase.class);

    private final PaymentController controller = new PaymentController(
            getPaymentByIdUseCase,
            getPaymentByBookingIdUseCase,
            getUserPaymentsUseCase,
            createCheckoutSessionUseCase);

    @Nested
    @DisplayName("annotation checks")
    class AnnotationChecks {

        @Test
        @DisplayName("getPaymentById requires authentication")
        void getPaymentById_requiresAuthentication() throws Exception {
            assertThat(PaymentController.class
                            .getDeclaredMethod(
                                    "getPaymentById",
                                    UUID.class,
                                    Authentication.class,
                                    GetPaymentByIdRequest.class)
                            .isAnnotationPresent(PreAuthorize.class))
                    .isTrue();
        }

        @Test
        @DisplayName("getPaymentByBookingId requires authentication")
        void getPaymentByBookingId_requiresAuthentication() throws Exception {
            assertThat(PaymentController.class
                            .getDeclaredMethod(
                                    "getPaymentByBookingId",
                                    UUID.class,
                                    Authentication.class,
                                    GetPaymentByBookingIdRequest.class)
                            .isAnnotationPresent(PreAuthorize.class))
                    .isTrue();
        }

        @Test
        @DisplayName("listByUser requires authentication")
        void listByUser_requiresAuthentication() throws Exception {
            assertThat(PaymentController.class
                            .getDeclaredMethod(
                                    "listByUser",
                                    UUID.class,
                                    Authentication.class,
                                    GetUserPaymentsRequest.class)
                            .isAnnotationPresent(PreAuthorize.class))
                    .isTrue();
        }

        @Test
        @DisplayName("createCheckout requires authentication")
        void createCheckout_requiresAuthentication() throws Exception {
            assertThat(PaymentController.class
                            .getDeclaredMethod("createCheckout", UUID.class, Authentication.class)
                            .isAnnotationPresent(PreAuthorize.class))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("pen-test")
    class PenTest {

        @Test
        @DisplayName("getPaymentById null authentication throws NullPointerException")
        void getPaymentById_nullAuthenticationThrowsNullPointerException() {
            UUID paymentId = UUID.fromString("22222222-2222-2222-2222-222222222222");

            assertThatThrownBy(() ->
                            controller.getPaymentById(paymentId, null, new GetPaymentByIdRequest()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName(
                "getPaymentById with malformed UUID in auth name throws IllegalArgumentException")
        void getPaymentById_malformedUuidInAuthNameThrowsIllegalArgumentException() {
            UUID paymentId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            Authentication auth = new UsernamePasswordAuthenticationToken("not-a-uuid", null);

            assertThatThrownBy(() ->
                            controller.getPaymentById(paymentId, auth, new GetPaymentByIdRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getPaymentByBookingId null authentication throws NullPointerException")
        void getPaymentByBookingId_nullAuthenticationThrowsNullPointerException() {
            UUID bookingId = UUID.fromString("22222222-2222-2222-2222-222222222222");

            assertThatThrownBy(() -> controller.getPaymentByBookingId(
                            bookingId, null, new GetPaymentByBookingIdRequest()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName(
                "getPaymentByBookingId with malformed UUID in auth name throws IllegalArgumentException")
        void getPaymentByBookingId_malformedUuidInAuthNameThrowsIllegalArgumentException() {
            UUID bookingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            Authentication auth = new UsernamePasswordAuthenticationToken("not-a-uuid", null);

            assertThatThrownBy(() -> controller.getPaymentByBookingId(
                            bookingId, auth, new GetPaymentByBookingIdRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
