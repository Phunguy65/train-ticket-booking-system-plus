package io.github.phunguy65.ttbs.backend.booking.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CancelBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.CreateBookingUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetBookingByIdUseCase;
import io.github.phunguy65.ttbs.backend.booking.application.usecase.GetUserBookingsUseCase;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.infrastructure.web.request.GetUserBookingsRequest;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.ErrorCode;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.FailData;
import io.github.phunguy65.ttbs.backend.shared.infrastructure.web.JsendResponse;
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
    private final GetBookingByIdUseCase getBookingByIdUseCase = mock(GetBookingByIdUseCase.class);
    private final GetUserBookingsUseCase getUserBookingsUseCase =
            mock(GetUserBookingsUseCase.class);

    private final BookingController controller = new BookingController(
            createBookingUseCase,
            cancelBookingUseCase,
            getBookingByIdUseCase,
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
}
