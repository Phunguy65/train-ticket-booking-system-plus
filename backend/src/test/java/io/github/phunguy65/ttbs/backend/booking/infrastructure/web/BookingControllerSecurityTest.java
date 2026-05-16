package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerResponse;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingDetailUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetUserBookingsUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.CreateBookingRequest;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetBookingDetailRequest;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetUserBookingsRequest;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@DisplayName("BookingController security")
class BookingControllerSecurityTest {

    private final CreateBookingUseCase createBookingUseCase = mock(CreateBookingUseCase.class);
    private final CancelBookingUseCase cancelBookingUseCase = mock(CancelBookingUseCase.class);
    private final GetBookingDetailUseCase getBookingDetailUseCase =
            mock(GetBookingDetailUseCase.class);
    private final GetUserBookingsUseCase getUserBookingsUseCase =
            mock(GetUserBookingsUseCase.class);
    private final BookingController controller = new BookingController(
            createBookingUseCase,
            cancelBookingUseCase,
            getBookingDetailUseCase,
            getUserBookingsUseCase);

    @Nested
    @DisplayName("pen-test")
    class PenTest {

        @Test
        @DisplayName("passes XSS payload in idempotency key to use case")
        void create_passesXssPayloadInIdempotencyKeyToUseCase() {
            UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID seatId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            String payload = "<script>alert('xss')</script>";
            CreateBookingRequest request = createRequest(scheduledTripId, seatId, payload);
            Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null);
            mockCreateRequestContext();
            when(createBookingUseCase.execute(request.toCommand(userId)))
                    .thenReturn(Result.success(bookingResponse(userId, scheduledTripId, seatId)));

            var result = controller.create(request, auth);

            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("passes SQL injection payload in idempotency key to use case")
        void create_passesSqlInjectionPayloadInIdempotencyKeyToUseCase() {
            UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID seatId = UUID.fromString("33333333-3333-3333-3333-333333333333");
            String payload = "'; DROP TABLE bookings; --";
            CreateBookingRequest request = createRequest(scheduledTripId, seatId, payload);
            Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null);
            mockCreateRequestContext();
            when(createBookingUseCase.execute(request.toCommand(userId)))
                    .thenReturn(Result.success(bookingResponse(userId, scheduledTripId, seatId)));

            var result = controller.create(request, auth);

            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().status()).isEqualTo("success");
            RequestContextHolder.resetRequestAttributes();
        }

        @Test
        @DisplayName("null authentication throws NullPointerException")
        void create_nullAuthenticationThrowsNullPointerException() {
            UUID scheduledTripId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UUID seatId = UUID.fromString("33333333-3333-3333-3333-333333333333");

            assertThatThrownBy(() -> controller.create(
                            createRequest(scheduledTripId, seatId, "idem-key"), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("listByUser null authentication throws NullPointerException")
        void listByUser_nullAuthenticationThrowsNullPointerException() {
            UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            GetUserBookingsRequest request = new GetUserBookingsRequest();

            assertThatThrownBy(() -> controller.listByUser(userId, null, request))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("getById null authentication throws NullPointerException")
        void getById_nullAuthenticationThrowsNullPointerException() {
            UUID bookingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
            GetBookingDetailRequest request = new GetBookingDetailRequest();

            assertThatThrownBy(() -> controller.getById(bookingId, null, request))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("listByUser with malformed UUID in auth name throws IllegalArgumentException")
        void listByUser_malformedUuidInAuthNameThrowsIllegalArgumentException() {
            UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            Authentication auth = new UsernamePasswordAuthenticationToken("not-a-uuid", null);

            assertThatThrownBy(
                            () -> controller.listByUser(userId, auth, new GetUserBookingsRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("annotation check")
    class AnnotationCheck {

        @Test
        @DisplayName("booking controller is not annotated with PreAuthorize")
        void bookingController_isNotAnnotatedWithPreAuthorize() {
            assertThat(BookingController.class.isAnnotationPresent(PreAuthorize.class))
                    .isFalse();
        }

        @Test
        @DisplayName("create requires authentication")
        void create_requiresAuthentication() throws Exception {
            assertThat(createMethod().isAnnotationPresent(PreAuthorize.class)).isTrue();
            assertThat(createMethod().getAnnotation(PreAuthorize.class).value())
                    .isEqualTo("isAuthenticated()");
        }

        @Test
        @DisplayName("listByUser requires authentication")
        void listByUser_requiresAuthentication() throws Exception {
            assertThat(BookingController.class
                            .getDeclaredMethod(
                                    "listByUser",
                                    UUID.class,
                                    Authentication.class,
                                    io.github.phunguy65.ttbs.backend.booking.infrastructure.web
                                            .request.GetUserBookingsRequest.class)
                            .isAnnotationPresent(PreAuthorize.class))
                    .isTrue();
        }

        @Test
        @DisplayName("getById requires authentication")
        void getById_requiresAuthentication() throws Exception {
            assertThat(BookingController.class
                            .getDeclaredMethod(
                                    "getById",
                                    UUID.class,
                                    Authentication.class,
                                    io.github.phunguy65.ttbs.backend.booking.infrastructure.web
                                            .request.GetBookingDetailRequest.class)
                            .isAnnotationPresent(PreAuthorize.class))
                    .isTrue();
        }

        @Test
        @DisplayName("cancel requires authentication")
        void cancel_requiresAuthentication() throws Exception {
            assertThat(BookingController.class
                            .getDeclaredMethod("cancel", UUID.class, Authentication.class)
                            .isAnnotationPresent(PreAuthorize.class))
                    .isTrue();
        }

        @Test
        @DisplayName("create declares Valid on request body parameter")
        void create_declaresValidOnRequestBodyParameter() throws Exception {
            assertThat(parameterAnnotationNames(createMethod(), 0)).contains(Valid.class.getName());
        }

        @Test
        @DisplayName("listByUser declares Valid on request parameter")
        void listByUser_declaresValidOnRequestParameter() throws Exception {
            Method listByUserMethod = BookingController.class.getDeclaredMethod(
                    "listByUser", UUID.class, Authentication.class, GetUserBookingsRequest.class);

            assertThat(parameterAnnotationNames(listByUserMethod, 2))
                    .contains(Valid.class.getName());
        }

        private Method createMethod() throws NoSuchMethodException {
            return BookingController.class.getDeclaredMethod(
                    "create", CreateBookingRequest.class, Authentication.class);
        }

        private List<String> parameterAnnotationNames(Method method, int index) {
            return Arrays.stream(method.getParameterAnnotations()[index])
                    .map(annotation -> annotation.annotationType().getName())
                    .toList();
        }
    }

    private CreateBookingRequest createRequest(
            UUID scheduledTripId, UUID seatId, String idempotencyKey) {
        return new CreateBookingRequest(
                scheduledTripId,
                List.of(seatId),
                List.of(new CreateBookingRequest.PassengerInput(
                        seatId, "Name", "ID001", LocalDate.of(1990, 1, 1), "Male")),
                idempotencyKey);
    }

    private BookingResponse bookingResponse(UUID userId, UUID scheduledTripId, UUID seatId) {
        return new BookingResponse(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                userId,
                scheduledTripId,
                new PassengerInfoResponse(
                        "Name",
                        "email@test.com",
                        "0900000000",
                        null,
                        "Male",
                        "ID001",
                        "123 Street"),
                List.of(new PassengerResponse(
                        seatId, "Name", "ID001", LocalDate.of(1990, 1, 1), "Male")),
                500000L,
                "VND",
                BookingStatus.HELD,
                Instant.now().plusSeconds(900),
                Instant.now());
    }

    private void mockCreateRequestContext() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", "/1.0/bookings");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
    }
}
