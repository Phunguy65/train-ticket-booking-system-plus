package io.github.phunguy65.ttbs.backend.booking.application.response;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingUserInfoSummary;
import org.springframework.stereotype.Component;

@Component
public class BookingResponseMapper {

    public BookingResponse fromBooking(Booking booking) {
        return new BookingResponse(
                booking.getBookingId().value(),
                booking.getUserId().value(),
                booking.getScheduledTripId().value(),
                toUserInfoResponse(booking.getUserInfo()),
                booking.getTotalPrice().toLong(),
                booking.getCurrency(),
                booking.getStatus(),
                booking.getPaymentDeadline(),
                booking.getCreatedAt());
    }

    public BookingResponse fromSummary(BookingSummary summary) {
        return new BookingResponse(
                summary.id(),
                summary.userId(),
                summary.scheduledTripId(),
                toUserInfoResponse(summary.userInfo()),
                summary.totalPrice(),
                summary.currency(),
                BookingStatus.valueOf(summary.status()),
                summary.paymentDeadline(),
                summary.createdAt());
    }

    private PassengerInfoResponse toUserInfoResponse(BookingUserInfo userInfo) {
        return new PassengerInfoResponse(
                userInfo.fullName(),
                userInfo.email(),
                userInfo.phone(),
                userInfo.dateOfBirth(),
                userInfo.gender(),
                userInfo.idDocumentNumber(),
                userInfo.addressLine());
    }

    private PassengerInfoResponse toUserInfoResponse(BookingUserInfoSummary userInfo) {
        return new PassengerInfoResponse(
                userInfo.fullName(),
                userInfo.email(),
                userInfo.phone(),
                userInfo.dateOfBirth(),
                userInfo.gender(),
                userInfo.idDocumentNumber(),
                userInfo.addressLine());
    }
}
