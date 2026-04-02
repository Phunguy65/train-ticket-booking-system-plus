package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetUserBookingsQuery;
import io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponseMapper;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserBookingsUseCase {

    private final BookingRepository bookingRepository;
    private final UserBookingResponseMapper userBookingResponseMapper;

    public GetUserBookingsUseCase(
            BookingRepository bookingRepository,
            UserBookingResponseMapper userBookingResponseMapper) {
        this.bookingRepository = bookingRepository;
        this.userBookingResponseMapper = userBookingResponseMapper;
    }

    @Transactional(readOnly = true)
    public Result<PageResponse<UserBookingResponse>, BookingError> execute(
            GetUserBookingsQuery query) {
        if (!query.userId().equals(query.requestingUserId())) {
            return Result.failure(new BookingError.Forbidden());
        }

        PageResponse<BookingSummary> bookings = bookingRepository.findByUserId(
                UserId.of(query.userId()),
                query.page(),
                query.size(),
                List.of(SortOrder.desc("createdAt"), SortOrder.desc("id")));
        return Result.success(PageResponse.of(
                bookings.content().stream()
                        .map(userBookingResponseMapper::fromSummary)
                        .toList(),
                bookings.page(),
                bookings.size(),
                bookings.hasNext(),
                bookings.total()));
    }
}
