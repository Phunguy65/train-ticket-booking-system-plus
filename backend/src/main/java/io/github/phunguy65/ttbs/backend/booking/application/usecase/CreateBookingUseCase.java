package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteQueryPort;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBookingUseCase {

    private static final long HOLD_DURATION_SECONDS = 15 * 60;

    private final BookingRepository bookingRepository;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;
    private final RouteQueryPort routeQueryPort;
    private final ApplicationEventPublisher eventPublisher;

    public CreateBookingUseCase(
            BookingRepository bookingRepository,
            RouteSeatAvailabilityPort seatAvailabilityPort,
            RouteQueryPort routeQueryPort,
            ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.routeQueryPort = routeQueryPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<BookingDto, BookingError> execute(CreateBookingCommand command) {
        var existing = bookingRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return Result.success(toDto(existing.get()));
        }

        UserId userId = UserId.of(command.userId());
        RouteId routeId = RouteId.of(command.routeId());

        var activeHold = bookingRepository.findActiveHoldByUserAndRoute(userId, routeId);
        if (activeHold.isPresent()) {
            return Result.failure(new BookingError.ActiveHoldExists());
        }

        Result<Void, RouteSeatAvailabilityError> holdResult =
                seatAvailabilityPort.holdSeats(routeId, command.seatIds());
        if (holdResult.isFailure()) {
            return Result.failure(new BookingError.SeatNotAvailable());
        }

        var routeOpt = routeQueryPort.findById(routeId);
        if (routeOpt.isEmpty()) {
            return Result.failure(new BookingError.RouteNotFound());
        }
        Money totalPrice = Money.vnd(
                routeOpt.get().getBasePrice().toLong() * command.seatIds().size());

        Instant paymentDeadline = Instant.now().plusSeconds(HOLD_DURATION_SECONDS);
        Booking booking = Booking.create(
                BookingId.of(UuidGenerator.generate()),
                userId,
                routeId,
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

    private BookingDto toDto(Booking booking) {
        return new BookingDto(
                booking.getBookingId().value(),
                booking.getUserId().value(),
                booking.getRouteId().value(),
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
