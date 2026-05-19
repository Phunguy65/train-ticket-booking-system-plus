package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.port.BookingConfigProvider;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerResponse;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingPassenger;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.shared.domain.event.SeatStatusChangedEvent;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBookingUseCase {

    private static final long HOLD_DURATION_SECONDS = 15 * 60;

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityManager seatAvailabilityPort;
    private final ScheduledTripRepository scheduledTripRepository;
    private final RouteTemplateRepository routeTemplateRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingConfigProvider bookingConfigProvider;

    public CreateBookingUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityManager seatAvailabilityPort,
            ScheduledTripRepository scheduledTripRepository,
            RouteTemplateRepository routeTemplateRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher,
            BookingConfigProvider bookingConfigProvider) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.scheduledTripRepository = scheduledTripRepository;
        this.routeTemplateRepository = routeTemplateRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.bookingConfigProvider = bookingConfigProvider;
    }

    @Transactional
    public Result<BookingResponse, BookingError> execute(CreateBookingCommand command) {
        var existing = bookingRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            Booking b = existing.get();
            return Result.success(toBookingResponse(b));
        }

        int maxSeats = bookingConfigProvider.getMaxSeatsPerBooking();
        if (command.seatIds().size() > maxSeats) {
            return Result.failure(
                    new BookingError.TooManySeats(command.seatIds().size(), maxSeats));
        }

        List<CreateBookingCommand.PassengerPayload> passengers = command.passengers();
        if (passengers == null || passengers.size() != command.seatIds().size()) {
            return Result.failure(new BookingError.PassengerSeatMismatch(
                    passengers == null ? 0 : passengers.size(),
                    command.seatIds().size()));
        }

        Set<String> seenIdDocs = new HashSet<>();
        for (CreateBookingCommand.PassengerPayload passenger : passengers) {
            if (passenger.idDocumentNumber() != null
                    && !seenIdDocs.add(passenger.idDocumentNumber())) {
                return Result.failure(new BookingError.DuplicatePassengerIdDocument(
                        passenger.idDocumentNumber()));
            }
        }

        Set<SeatId> passengerSeatIds = passengers.stream()
                .map(CreateBookingCommand.PassengerPayload::seatId)
                .collect(Collectors.toSet());
        if (passengerSeatIds.size() != passengers.size()) {
            return Result.failure(new BookingError.DuplicatePassengerSeatAssignment());
        }

        Set<SeatId> selectedSeatIds = command.seatIds().stream().collect(Collectors.toSet());
        for (CreateBookingCommand.PassengerPayload passenger : passengers) {
            if (!selectedSeatIds.contains(passenger.seatId())) {
                return Result.failure(new BookingError.InvalidPassengerSeatAssignment(
                        passenger.seatId().value().toString()));
            }
        }

        UserId userId = UserId.of(command.userId());
        ScheduledTripId scheduledTripId = ScheduledTripId.of(command.scheduledTripId());
        var userInfoOpt = userRepository.findSummaryById(userId).map(this::toBookingUserInfo);
        if (userInfoOpt.isEmpty()) {
            return Result.failure(new BookingError.UserNotFound());
        }

        var activeHold =
                bookingRepository.findActiveHoldByUserAndScheduledTrip(userId, scheduledTripId);
        if (activeHold.isPresent()) {
            return Result.failure(new BookingError.ActiveHoldExists());
        }

        var scheduledTripOpt = scheduledTripRepository.findById(scheduledTripId);
        if (scheduledTripOpt.isEmpty()) {
            return Result.failure(new BookingError.ScheduledTripNotFound());
        }
        var scheduledTrip = scheduledTripOpt.get();
        var routeTemplateOpt = routeTemplateRepository.findById(scheduledTrip.getRouteTemplateId());
        if (routeTemplateOpt.isEmpty()) {
            return Result.failure(new BookingError.ScheduledTripNotFound());
        }
        Money pricePerSeat = routeTemplateOpt.get().getBasePrice();
        Money totalPrice = Money.vnd(pricePerSeat.toLong() * command.seatIds().size());

        BookingUserInfo bookerInfo = userInfoOpt.get();

        List<BookingPassenger> bookingPassengers = passengers.stream()
                .map(p -> BookingPassenger.of(
                        p.seatId(),
                        p.fullName(),
                        p.idDocumentNumber(),
                        p.dateOfBirth(),
                        p.gender()))
                .toList();

        Instant paymentDeadline = Instant.now().plusSeconds(HOLD_DURATION_SECONDS);
        Booking booking = Booking.create(
                BookingId.of(UuidGenerator.generate()),
                userId,
                scheduledTripId,
                bookerInfo,
                bookingPassengers,
                totalPrice,
                command.idempotencyKey(),
                paymentDeadline);

        Booking saved = bookingRepository.save(booking);

        Result<Void, RouteSeatAvailabilityError> holdResult =
                seatAvailabilityPort.holdSeatsWithBookingId(
                        scheduledTripId,
                        command.seatIds(),
                        saved.getBookingId().value(),
                        pricePerSeat);
        if (holdResult.isFailure()) {
            return Result.failure(new BookingError.SeatNotAvailable());
        }

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        List<RouteSeatAvailability> bookedSeats =
                seatAvailabilityPort.findByBookingId(saved.getBookingId().value());
        if (!bookedSeats.isEmpty()) {
            List<SeatStatusChangedEvent.SeatChange> changes = bookedSeats.stream()
                    .map(seat -> new SeatStatusChangedEvent.SeatChange(
                            seat.getSeatId().value(),
                            seat.getStatus().name(),
                            saved.getBookingId().value()))
                    .toList();
            SeatStatusChangedEvent sseEvent =
                    new SeatStatusChangedEvent(scheduledTripId.value(), changes, Instant.now());
            eventPublisher.publishEvent(sseEvent);
        }

        return Result.success(toBookingResponse(saved));
    }

    private BookingUserInfo toBookingUserInfo(UserSummary summary) {
        return BookingUserInfo.of(
                summary.fullName(),
                summary.email(),
                summary.phone(),
                summary.dateOfBirth(),
                summary.gender(),
                summary.idDocumentNumber(),
                summary.addressLine());
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

    private BookingResponse toBookingResponse(Booking booking) {
        List<PassengerResponse> passengerResponses = booking.getPassengers().stream()
                .map(p -> new PassengerResponse(
                        p.seatId().value(),
                        p.fullName(),
                        p.idDocumentNumber(),
                        p.dateOfBirth(),
                        p.gender()))
                .toList();

        return new BookingResponse(
                booking.getBookingId().value(),
                booking.getUserId().value(),
                booking.getScheduledTripId().value(),
                toPassengerInfo(booking.getBookerInfo()),
                passengerResponses,
                booking.getTotalPrice().toLong(),
                booking.getCurrency(),
                booking.getStatus(),
                booking.getPaymentDeadline(),
                booking.getCreatedAt());
    }
}
