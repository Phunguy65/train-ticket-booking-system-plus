package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.response.BookingResponse;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityManager;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteTemplateRepository;
import io.github.phunguy65.ttbs.backend.train.domain.repository.ScheduledTripRepository;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
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
    private final ApplicationEventPublisher eventPublisher;

    public CreateBookingUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityManager seatAvailabilityPort,
            ScheduledTripRepository scheduledTripRepository,
            RouteTemplateRepository routeTemplateRepository,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.scheduledTripRepository = scheduledTripRepository;
        this.routeTemplateRepository = routeTemplateRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<BookingResponse, BookingError> execute(CreateBookingCommand command) {
        var existing = bookingRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return Result.success(toDto(existing.get()));
        }

        UserId userId = UserId.of(command.userId());
        ScheduledTripId scheduledTripId = ScheduledTripId.of(command.routeId());

        var activeHold =
                bookingRepository.findActiveHoldByUserAndScheduledTrip(userId, scheduledTripId);
        if (activeHold.isPresent()) {
            return Result.failure(new BookingError.ActiveHoldExists());
        }

        Result<Void, RouteSeatAvailabilityError> holdResult =
                seatAvailabilityPort.holdSeats(scheduledTripId, command.seatIds());
        if (holdResult.isFailure()) {
            return Result.failure(new BookingError.SeatNotAvailable());
        }

        var scheduledTripOpt = scheduledTripRepository.findById(scheduledTripId);
        if (scheduledTripOpt.isEmpty()) {
            return Result.failure(new BookingError.RouteNotFound());
        }
        var scheduledTrip = scheduledTripOpt.get();
        var routeTemplateOpt = routeTemplateRepository.findById(scheduledTrip.getRouteTemplateId());
        if (routeTemplateOpt.isEmpty()) {
            return Result.failure(new BookingError.RouteNotFound());
        }
        Money totalPrice = Money.vnd(routeTemplateOpt.get().getBasePrice().toLong()
                * command.seatIds().size());

        Instant paymentDeadline = Instant.now().plusSeconds(HOLD_DURATION_SECONDS);
        Booking booking = Booking.create(
                BookingId.of(UuidGenerator.generate()),
                userId,
                scheduledTripId,
                command.passengerName(),
                command.passengerEmail(),
                command.passengerPhone(),
                totalPrice,
                totalPrice.getCurrency().getCurrencyCode(),
                command.idempotencyKey(),
                paymentDeadline);

        Booking saved = bookingRepository.save(booking);

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        return Result.success(toDto(saved));
    }

    private BookingResponse toDto(Booking booking) {
        return new BookingResponse(
                booking.getBookingId().value(),
                booking.getUserId().value(),
                booking.getScheduledTripId().value(),
                booking.getPassengerName(),
                booking.getPassengerEmail(),
                booking.getPassengerPhone(),
                booking.getTotalPrice().toLong(),
                booking.getCurrency(),
                booking.getStatus(),
                booking.getPaymentDeadline(),
                booking.getCreatedAt());
    }
}
