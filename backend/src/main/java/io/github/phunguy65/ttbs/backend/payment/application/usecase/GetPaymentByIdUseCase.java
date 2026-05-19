package io.github.phunguy65.ttbs.backend.payment.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingPassenger;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.payment.application.query.GetPaymentByIdQuery;
import io.github.phunguy65.ttbs.backend.payment.application.response.PaymentDetailResponse;
import io.github.phunguy65.ttbs.backend.payment.domain.error.PaymentError;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentId;
import io.github.phunguy65.ttbs.backend.payment.domain.model.PaymentStatus;
import io.github.phunguy65.ttbs.backend.payment.domain.projection.PaymentSummary;
import io.github.phunguy65.ttbs.backend.payment.domain.repository.PaymentRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.projection.BookedSeatSummary;
import io.github.phunguy65.ttbs.backend.train.domain.projection.ScheduledTripEnrichedSummary;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaymentByIdUseCase {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final ScheduledTripRepository scheduledTripRepository;
    private final RouteSeatAvailabilityRepository routeSeatAvailabilityRepository;

    public GetPaymentByIdUseCase(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            ScheduledTripRepository scheduledTripRepository,
            RouteSeatAvailabilityRepository routeSeatAvailabilityRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.scheduledTripRepository = scheduledTripRepository;
        this.routeSeatAvailabilityRepository = routeSeatAvailabilityRepository;
    }

    @Transactional(readOnly = true)
    public Result<PaymentDetailResponse, PaymentError> execute(GetPaymentByIdQuery query) {
        PaymentSummary payment = paymentRepository
                .findSummaryById(PaymentId.of(query.paymentId()))
                .orElse(null);

        if (payment == null) {
            return Result.failure(new PaymentError.PaymentNotFound());
        }

        if (!payment.userId().equals(query.requestingUserId())) {
            return Result.failure(new PaymentError.Forbidden());
        }

        BookingId bookingId = BookingId.of(payment.bookingId());
        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        PaymentDetailResponse.BookingForTicket bookingForTicket = null;
        if (booking != null) {
            ScheduledTripEnrichedSummary trip = scheduledTripRepository
                    .findEnrichedByIdIncludingDeleted(booking.getScheduledTripId())
                    .orElse(null);
            List<BookedSeatSummary> seats =
                    routeSeatAvailabilityRepository.findBookedSeatSummariesByBookingId(bookingId);

            Map<UUID, BookedSeatSummary> seatMap =
                    seats.stream().collect(Collectors.toMap(BookedSeatSummary::seatId, s -> s));

            List<PaymentDetailResponse.PassengerWithSeat> passengersWithSeats =
                    booking.getPassengers().stream()
                            .map(p -> toPassengerWithSeat(p, seatMap))
                            .toList();

            bookingForTicket = new PaymentDetailResponse.BookingForTicket(
                    booking.getBookingId().value(),
                    booking.getStatus(),
                    toBookerInfo(booking.getBookerInfo()),
                    passengersWithSeats,
                    seats.stream().map(this::toSeatInfo).toList(),
                    trip == null ? null : toTripInfo(trip));
        }

        return Result.success(new PaymentDetailResponse(
                payment.id(),
                payment.bookingId(),
                PaymentStatus.valueOf(payment.status().toUpperCase(Locale.ROOT)),
                payment.checkoutUrl(),
                BigDecimal.valueOf(payment.amount()),
                payment.currency(),
                payment.createdAt(),
                bookingForTicket));
    }

    private PaymentDetailResponse.BookerInfo toBookerInfo(BookingUserInfo userInfo) {
        return new PaymentDetailResponse.BookerInfo(
                userInfo.fullName(),
                userInfo.email(),
                userInfo.phone(),
                userInfo.dateOfBirth(),
                userInfo.gender(),
                userInfo.idDocumentNumber());
    }

    private PaymentDetailResponse.PassengerWithSeat toPassengerWithSeat(
            BookingPassenger passenger, Map<UUID, BookedSeatSummary> seatMap) {
        BookedSeatSummary seat = seatMap.get(passenger.seatId().value());
        return new PaymentDetailResponse.PassengerWithSeat(
                passenger.seatId().value(),
                seat != null ? seat.coachNumber() : 0,
                seat != null ? seat.seatNumber() : "",
                passenger.fullName(),
                passenger.idDocumentNumber(),
                passenger.dateOfBirth(),
                passenger.gender());
    }

    private PaymentDetailResponse.SeatInfo toSeatInfo(BookedSeatSummary seat) {
        return new PaymentDetailResponse.SeatInfo(
                seat.seatId(), seat.coachNumber(), seat.seatNumber());
    }

    private PaymentDetailResponse.TripInfo toTripInfo(ScheduledTripEnrichedSummary trip) {
        return new PaymentDetailResponse.TripInfo(
                trip.trainName(),
                trip.trainNumber(),
                trip.originStationName(),
                trip.destinationStationName(),
                trip.departureTime(),
                trip.arrivalTime());
    }
}
