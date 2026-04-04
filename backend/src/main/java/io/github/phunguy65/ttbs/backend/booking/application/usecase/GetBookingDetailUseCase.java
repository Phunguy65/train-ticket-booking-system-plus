package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.query.GetBookingDetailQuery;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingDetailResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PaymentDetailResponse;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.response.ScheduledTripDetailResponse;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.projection.BookedSeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBookingDetailUseCase {

    private final BookingRepository bookingRepository;
    private final ScheduledTripRepository scheduledTripRepository;
    private final PaymentRepository paymentRepository;
    private final RouteSeatAvailabilityRepository routeSeatAvailabilityRepository;

    public GetBookingDetailUseCase(
            BookingRepository bookingRepository,
            ScheduledTripRepository scheduledTripRepository,
            PaymentRepository paymentRepository,
            RouteSeatAvailabilityRepository routeSeatAvailabilityRepository) {
        this.bookingRepository = bookingRepository;
        this.scheduledTripRepository = scheduledTripRepository;
        this.paymentRepository = paymentRepository;
        this.routeSeatAvailabilityRepository = routeSeatAvailabilityRepository;
    }

    @Transactional(readOnly = true)
    public Result<BookingDetailResponse, BookingError> execute(GetBookingDetailQuery query) {
        BookingId bookingId = BookingId.of(query.bookingId());
        UserId requestingUserId = UserId.of(query.requestingUserId());
        var booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return Result.failure(new BookingError.BookingNotFound());
        }
        if (!booking.getUserId().equals(requestingUserId)) {
            return Result.failure(new BookingError.Forbidden());
        }

        ScheduledTripEnrichedSummary trip = scheduledTripRepository
                .findEnrichedByIdIncludingDeleted(booking.getScheduledTripId())
                .orElse(null);
        PaymentSummary payment =
                paymentRepository.findSummaryByBookingId(bookingId).orElse(null);
        List<BookedSeatSummary> seats =
                routeSeatAvailabilityRepository.findBookedSeatSummariesByBookingId(
                        booking.getBookingId());

        return Result.success(new BookingDetailResponse(
                booking.getBookingId().value(),
                booking.getUserId().value(),
                booking.getScheduledTripId().value(),
                toPassengerInfo(booking.getUserInfo()),
                booking.getTotalPrice().toLong(),
                booking.getCurrency(),
                booking.getStatus(),
                booking.getPaymentDeadline(),
                booking.getCreatedAt(),
                trip == null
                        ? null
                        : BookingDetailResponse.Trip.fromScheduledTripDetail(toTripResponse(trip)),
                payment == null ? null : toPaymentDetail(payment),
                seats.stream().map(this::toSeatDetail).toList()));
    }

    private PassengerInfoResponse toPassengerInfo(BookingUserInfo userInfo) {
        return new PassengerInfoResponse(
                userInfo.fullName(),
                userInfo.email(),
                userInfo.phone(),
                userInfo.dateOfBirth(),
                userInfo.gender(),
                userInfo.idDocumentNumber(),
                userInfo.addressLine());
    }

    private PaymentDetailResponse toPaymentDetail(PaymentSummary payment) {
        return new PaymentDetailResponse(
                payment.id(),
                parsePaymentStatus(payment.status()),
                payment.checkoutUrl(),
                payment.amount(),
                payment.currency(),
                payment.stripePaymentIntentId(),
                payment.createdAt());
    }

    private BookingDetailResponse.Seat toSeatDetail(BookedSeatSummary seat) {
        return new BookingDetailResponse.Seat(
                seat.seatId(),
                seat.coachId(),
                seat.coachNumber(),
                seat.seatNumber(),
                parseRouteSeatAvailabilityStatus(seat.status()),
                seat.priceAtBooking());
    }

    private PaymentStatus parsePaymentStatus(String rawStatus) {
        return PaymentStatus.valueOf(rawStatus.toUpperCase(Locale.ROOT));
    }

    private RouteSeatAvailabilityStatus parseRouteSeatAvailabilityStatus(String rawStatus) {
        return RouteSeatAvailabilityStatus.valueOf(rawStatus.toUpperCase(Locale.ROOT));
    }

    private ScheduledTripDetailResponse toTripResponse(ScheduledTripEnrichedSummary scheduledTrip) {
        return new ScheduledTripDetailResponse(
                scheduledTrip.id(),
                scheduledTrip.routeTemplateId(),
                scheduledTrip.trainId(),
                scheduledTrip.departureTime(),
                scheduledTrip.arrivalTime(),
                io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripStatus.valueOf(
                        scheduledTrip.status()),
                scheduledTrip.createdAt(),
                scheduledTrip.trainNumber() == null
                                || scheduledTrip.trainName() == null
                                || scheduledTrip.trainTotalSeats() == null
                        ? null
                        : new ScheduledTripDetailResponse.Train(
                                scheduledTrip.trainId(),
                                scheduledTrip.trainNumber(),
                                scheduledTrip.trainName(),
                                scheduledTrip.trainTotalSeats()),
                new ScheduledTripDetailResponse.Route(
                        scheduledTrip.routeTemplateId(),
                        scheduledTrip.routeBasePrice(),
                        scheduledTrip.routeCurrency(),
                        new ScheduledTripDetailResponse.Station(
                                scheduledTrip.originStationId(),
                                scheduledTrip.originStationCode(),
                                scheduledTrip.originStationName(),
                                scheduledTrip.originStationCity()),
                        new ScheduledTripDetailResponse.Station(
                                scheduledTrip.destinationStationId(),
                                scheduledTrip.destinationStationCode(),
                                scheduledTrip.destinationStationName(),
                                scheduledTrip.destinationStationCity())));
    }
}
