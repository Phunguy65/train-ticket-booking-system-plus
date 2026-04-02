package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import org.springframework.stereotype.Component;

@Component
public class UserBookingResponseMapper {

    public UserBookingResponse fromBooking(Booking booking) {
        return new UserBookingResponse(
                booking.getBookingId().value(),
                booking.getUserId().value(),
                booking.getScheduledTripId().value(),
                booking.getTotalPrice().toLong(),
                booking.getCurrency(),
                booking.getStatus(),
                booking.getPaymentDeadline(),
                booking.getCreatedAt());
    }

    public UserBookingResponse fromSummary(BookingSummary summary) {
        return new UserBookingResponse(
                summary.id(),
                summary.userId(),
                summary.scheduledTripId(),
                summary.totalPrice(),
                summary.currency(),
                io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus.valueOf(
                        summary.status()),
                summary.paymentDeadline(),
                summary.createdAt());
    }
}
