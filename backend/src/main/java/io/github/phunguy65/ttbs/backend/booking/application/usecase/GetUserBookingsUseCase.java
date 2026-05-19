package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetUserBookingsQuery;
import io.github.phunguy65.ttbs.backend.booking.application.response.UserBookingResponse;
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

    public GetUserBookingsUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
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
                bookings.content().stream().map(this::toUserBookingResponse).toList(),
                bookings.page(),
                bookings.size(),
                bookings.hasNext(),
                bookings.total()));
    }

    private UserBookingResponse toUserBookingResponse(BookingSummary s) {
        return new UserBookingResponse(
                s.id(),
                s.userId(),
                s.scheduledTripId(),
                s.totalPrice(),
                s.currency(),
                io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus.valueOf(
                        s.status()),
                s.paymentDeadline(),
                s.createdAt());
    }
}
