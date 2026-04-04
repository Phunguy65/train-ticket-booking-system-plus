package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.application.response.PassengerInfoResponse;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
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
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
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

    public CreateBookingUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityManager seatAvailabilityPort,
            ScheduledTripRepository scheduledTripRepository,
            RouteTemplateRepository routeTemplateRepository,
            UserRepository userRepository,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.scheduledTripRepository = scheduledTripRepository;
        this.routeTemplateRepository = routeTemplateRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<BookingResponse, BookingError> execute(CreateBookingCommand command) {
        var existing = bookingRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            Booking b = existing.get();
            return Result.success(new BookingResponse(
                    b.getBookingId().value(),
                    b.getUserId().value(),
                    b.getScheduledTripId().value(),
                    toPassengerInfo(b.getUserInfo()),
                    b.getTotalPrice().toLong(),
                    b.getCurrency(),
                    b.getStatus(),
                    b.getPaymentDeadline(),
                    b.getCreatedAt()));
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

        BookingUserInfo userInfo = userInfoOpt.get();

        Instant paymentDeadline = Instant.now().plusSeconds(HOLD_DURATION_SECONDS);
        Booking booking = Booking.create(
                BookingId.of(UuidGenerator.generate()),
                userId,
                scheduledTripId,
                userInfo,
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

        return Result.success(new BookingResponse(
                saved.getBookingId().value(),
                saved.getUserId().value(),
                saved.getScheduledTripId().value(),
                toPassengerInfo(saved.getUserInfo()),
                saved.getTotalPrice().toLong(),
                saved.getCurrency(),
                saved.getStatus(),
                saved.getPaymentDeadline(),
                saved.getCreatedAt()));
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
}
