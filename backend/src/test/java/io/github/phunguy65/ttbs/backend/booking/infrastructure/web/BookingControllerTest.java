package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PaymentDetailResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingDetailUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetUserBookingsUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetBookingDetailRequest;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetUserBookingsRequest;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class BookingControllerTest {

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

    @Test
    void listByUserReturnsPagedBookingHistory() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        GetUserBookingsRequest request = new GetUserBookingsRequest();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        UserBookingResponse booking = new UserBookingResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                userId,
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                450000,
                "VND",
                BookingStatus.CONFIRMED,
                Instant.parse("2026-04-02T10:00:00Z"),
                Instant.parse("2026-04-01T09:00:00Z"));
        PageResponse<UserBookingResponse> page = PageResponse.of(List.of(booking), 0, 20, false, 1);
        when(getUserBookingsUseCase.execute(request.toQuery(userId, userId)))
                .thenReturn(Result.success(page));

        @SuppressWarnings("unchecked")
        JsendResponse<PageResponse<UserBookingResponse>> response =
                (JsendResponse<PageResponse<UserBookingResponse>>)
                        controller.listByUser(userId, auth, request).getBody();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isNull();
        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().page()).isZero();
        assertThat(response.data().size()).isEqualTo(20);
        assertThat(response.data().total()).isEqualTo(1);
        assertThat(response.data().hasNext()).isFalse();
        assertThat(response.data().hasPrevious()).isFalse();
        assertThat(response.data().content().getFirst()).isEqualTo(booking);
    }

    @Test
    void listByUserReturnsForbiddenWhenUseCaseRejectsAccess() {
        UUID pathUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID authUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        GetUserBookingsRequest request = new GetUserBookingsRequest();
        Authentication auth = new UsernamePasswordAuthenticationToken(authUserId.toString(), null);
        when(getUserBookingsUseCase.execute(request.toQuery(pathUserId, authUserId)))
                .thenReturn(Result.failure(new BookingError.Forbidden()));

        var response = controller.listByUser(pathUserId, auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        @SuppressWarnings("unchecked")
        JsendResponse<FailData> body = (JsendResponse<FailData>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("fail");
        assertThat(body.data().message())
                .isEqualTo("You are not allowed to perform this action on this booking");
        assertThat(body.data().code()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void getByIdReturnsBookingDetail() {
        UUID bookingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        GetBookingDetailRequest request = new GetBookingDetailRequest();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        BookingDetailResponse dto = new BookingDetailResponse(
                bookingId,
                userId,
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                new PassengerInfoResponse(
                        "Nguyen Van A",
                        "a@example.com",
                        "0900000000",
                        null,
                        "MALE",
                        "0123456789",
                        "123 Test Street"),
                List.of(new PassengerResponse(
                        UUID.fromString("99999999-9999-9999-9999-999999999999"),
                        "Nguyen Van A",
                        "0123456789",
                        null,
                        "MALE")),
                450000,
                "VND",
                BookingStatus.CONFIRMED,
                Instant.parse("2026-04-02T10:00:00Z"),
                Instant.parse("2026-04-01T09:00:00Z"),
                new BookingDetailResponse.Trip(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        UUID.fromString("55555555-5555-5555-5555-555555555555"),
                        Instant.parse("2026-05-01T08:00:00Z"),
                        Instant.parse("2026-05-01T12:00:00Z"),
                        "SCHEDULED",
                        Instant.parse("2026-04-01T09:00:00Z"),
                        new ScheduledTripDetailResponse.Train(
                                UUID.fromString("55555555-5555-5555-5555-555555555555"),
                                "SE1",
                                "North-South Express",
                                200),
                        new ScheduledTripDetailResponse.Route(
                                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                                450000,
                                "VND",
                                new ScheduledTripDetailResponse.Station(
                                        UUID.fromString("66666666-6666-6666-6666-666666666666"),
                                        "SGN",
                                        "Sai Gon",
                                        "Ho Chi Minh"),
                                new ScheduledTripDetailResponse.Station(
                                        UUID.fromString("77777777-7777-7777-7777-777777777777"),
                                        "DAD",
                                        "Da Nang",
                                        "Da Nang"))),
                new PaymentDetailResponse(
                        UUID.fromString("88888888-8888-8888-8888-888888888888"),
                        PaymentStatus.PENDING,
                        "https://checkout.test/session",
                        450000,
                        "VND",
                        "pi_123",
                        Instant.parse("2026-04-01T09:00:00Z")),
                List.of(new BookingDetailResponse.Seat(
                        UUID.fromString("99999999-9999-9999-9999-999999999999"),
                        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                        1,
                        "A1",
                        RouteSeatAvailabilityStatus.HELD,
                        225000L)));
        when(getBookingDetailUseCase.execute(request.toQuery(bookingId, userId)))
                .thenReturn(Result.success(dto));

        var response = controller.getById(bookingId, auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        JsendResponse<BookingDetailResponse> body =
                (JsendResponse<BookingDetailResponse>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo("success");
        assertThat(body.data().trip()).isNotNull();
        assertThat(body.data().payment()).isNotNull();
        assertThat(body.data().seats()).hasSize(1);
    }

    @Test
    void getByIdReturnsForbiddenWhenUseCaseRejectsAccess() {
        UUID bookingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        GetBookingDetailRequest request = new GetBookingDetailRequest();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        when(getBookingDetailUseCase.execute(request.toQuery(bookingId, userId)))
                .thenReturn(Result.failure(new BookingError.Forbidden()));

        var response = controller.getById(bookingId, auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        @SuppressWarnings("unchecked")
        JsendResponse<FailData> body = (JsendResponse<FailData>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.data().code()).isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void getByIdReturnsNotFoundWhenBookingMissing() {
        UUID bookingId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        GetBookingDetailRequest request = new GetBookingDetailRequest();
        Authentication auth = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        when(getBookingDetailUseCase.execute(request.toQuery(bookingId, userId)))
                .thenReturn(Result.failure(new BookingError.BookingNotFound()));

        var response = controller.getById(bookingId, auth, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        @SuppressWarnings("unchecked")
        JsendResponse<FailData> body = (JsendResponse<FailData>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.data().code()).isEqualTo(ErrorCode.BOOKING_NOT_FOUND);
    }
}
