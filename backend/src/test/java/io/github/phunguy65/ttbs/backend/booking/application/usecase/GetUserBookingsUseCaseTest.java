package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetUserBookingsQuery;
import io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingUserInfoSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetUserBookingsUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final BookingRepository bookingRepository = mock(BookingRepository.class);

    private final GetUserBookingsUseCase useCase = new GetUserBookingsUseCase(bookingRepository);

    @Test
    void returnsForbiddenWhenAuthenticatedUserDoesNotMatchPathUser() {
        GetUserBookingsQuery query = new GetUserBookingsQuery(
                USER_ID, UUID.fromString("22222222-2222-2222-2222-222222222222"), 0, 20);

        Result<PageResponse<UserBookingResponse>, BookingError> result = useCase.execute(query);

        assertThat(result).isEqualTo(Result.failure(new BookingError.Forbidden()));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void returnsPagedBookingsNewestFirstUsingDefaultSort() {
        BookingSummary booking = booking(
                "33333333-3333-3333-3333-333333333333",
                "44444444-4444-4444-4444-444444444444",
                BookingStatus.CONFIRMED,
                450000,
                "2026-04-02T10:00:00Z",
                "2026-04-01T09:00:00Z");
        PageResponse<BookingSummary> page = PageResponse.of(List.of(booking), 0, 20, false, 1);
        when(bookingRepository.findByUserId(
                        eq(UserId.of(USER_ID)),
                        eq(0),
                        eq(20),
                        eq(List.of(SortOrder.desc("createdAt"), SortOrder.desc("id")))))
                .thenReturn(page);

        Result<PageResponse<UserBookingResponse>, BookingError> result =
                useCase.execute(new GetUserBookingsQuery(USER_ID, USER_ID, 0, 20));

        assertThat(result.isSuccess()).isTrue();
        PageResponse<UserBookingResponse> response =
                ((Result.Success<PageResponse<UserBookingResponse>, BookingError>) result).value();
        assertThat(response.content())
                .containsExactly(new UserBookingResponse(
                        booking.id(),
                        booking.userId(),
                        booking.scheduledTripId(),
                        booking.totalPrice(),
                        booking.currency(),
                        BookingStatus.valueOf(booking.status()),
                        booking.paymentDeadline(),
                        booking.createdAt()));
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
        verify(bookingRepository)
                .findByUserId(
                        UserId.of(USER_ID),
                        0,
                        20,
                        List.of(SortOrder.desc("createdAt"), SortOrder.desc("id")));
    }

    @Test
    void returnsEmptyPageWhenUserHasNoBookings() {
        when(bookingRepository.findByUserId(
                        eq(UserId.of(USER_ID)),
                        eq(0),
                        eq(20),
                        eq(List.of(SortOrder.desc("createdAt"), SortOrder.desc("id")))))
                .thenReturn(PageResponse.empty(20));

        Result<PageResponse<UserBookingResponse>, BookingError> result =
                useCase.execute(new GetUserBookingsQuery(USER_ID, USER_ID, 0, 20));

        assertThat(result.isSuccess()).isTrue();
        PageResponse<UserBookingResponse> response =
                ((Result.Success<PageResponse<UserBookingResponse>, BookingError>) result).value();
        assertThat(response.content()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isFalse();
    }

    @Test
    void preservesPaginationMetadataAndContentOrderForLaterPages() {
        BookingSummary newerBooking = booking(
                "33333333-3333-3333-3333-333333333333",
                "44444444-4444-4444-4444-444444444444",
                BookingStatus.HELD,
                550000,
                "2026-04-03T10:00:00Z",
                "2026-04-02T09:00:00Z");
        BookingSummary olderBooking = booking(
                "55555555-5555-5555-5555-555555555555",
                "66666666-6666-6666-6666-666666666666",
                BookingStatus.CANCELLED,
                350000,
                "2026-04-04T10:00:00Z",
                "2026-04-02T09:00:00Z");
        PageResponse<BookingSummary> page =
                PageResponse.of(List.of(newerBooking, olderBooking), 1, 20, true, 42);
        when(bookingRepository.findByUserId(
                        eq(UserId.of(USER_ID)),
                        eq(1),
                        eq(20),
                        eq(List.of(SortOrder.desc("createdAt"), SortOrder.desc("id")))))
                .thenReturn(page);

        Result<PageResponse<UserBookingResponse>, BookingError> result =
                useCase.execute(new GetUserBookingsQuery(USER_ID, USER_ID, 1, 20));

        PageResponse<UserBookingResponse> response =
                ((Result.Success<PageResponse<UserBookingResponse>, BookingError>) result).value();
        assertThat(response.content())
                .extracting(UserBookingResponse::id)
                .containsExactly(newerBooking.id(), olderBooking.id());
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(42);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.hasPrevious()).isTrue();
    }

    private BookingSummary booking(
            String bookingId,
            String scheduledTripId,
            BookingStatus status,
            long totalPrice,
            String paymentDeadline,
            String createdAt) {
        return new BookingSummary(
                UUID.fromString(bookingId),
                USER_ID,
                UUID.fromString(scheduledTripId),
                new BookingUserInfoSummary(
                        "Nguyen Van A", "a@example.com", "0900000000", null, null, null, null),
                List.of(),
                totalPrice,
                "VND",
                status.name(),
                Instant.parse(paymentDeadline),
                Instant.parse(createdAt));
    }
}
