package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetBookingByIdQuery;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponseMapper;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBookingByIdUseCase {

    private final BookingRepository bookingRepository;
    private final BookingResponseMapper bookingResponseMapper;

    public GetBookingByIdUseCase(
            BookingRepository bookingRepository, BookingResponseMapper bookingResponseMapper) {
        this.bookingRepository = bookingRepository;
        this.bookingResponseMapper = bookingResponseMapper;
    }

    @Transactional(readOnly = true)
    public Result<BookingResponse, BookingError> execute(GetBookingByIdQuery query) {
        BookingId bookingId = BookingId.of(query.bookingId());
        UserId requestingUserId = UserId.of(query.requestingUserId());
        var booking = bookingRepository.findSummaryById(bookingId).orElse(null);
        if (booking == null) {
            return Result.failure(new BookingError.BookingNotFound());
        }
        if (!booking.userId().equals(requestingUserId.value())) {
            return Result.failure(new BookingError.Forbidden());
        }
        return Result.success(bookingResponseMapper.fromSummary(booking));
    }
}
