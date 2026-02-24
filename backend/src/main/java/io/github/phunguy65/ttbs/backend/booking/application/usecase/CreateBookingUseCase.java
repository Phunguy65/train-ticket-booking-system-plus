package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateBookingUseCase {

    private static final String DEFAULT_CURRENCY = "VND";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.ZERO;

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;

    public CreateBookingUseCase(
            BookingRepository bookingRepository,
            ApplicationEventPublisher eventPublisher,
            RouteSeatAvailabilityPort seatAvailabilityPort) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
        this.seatAvailabilityPort = seatAvailabilityPort;
    }

    @Transactional
    public Result<BookingDto, BookingError> execute(CreateBookingCommand command) {
        Optional<Booking> existing =
                bookingRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return Result.success(toDto(existing.get()));
        }

        var reserveResult = seatAvailabilityPort.reserveSeat(
                RouteId.of(command.routeId()), SeatId.of(command.seatId()));

        if (reserveResult.isFailure()) {
            return Result.failure(new BookingError.SeatNotAvailable());
        }

        Booking booking = Booking.create(
                command.userId(),
                command.routeId(),
                command.seatId(),
                DEFAULT_PRICE,
                DEFAULT_CURRENCY,
                command.idempotencyKey());

        Booking saved = bookingRepository.save(booking);

        for (DomainEvent event : booking.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        booking.clearDomainEvents();

        return Result.success(toDto(saved));
    }

    private BookingDto toDto(Booking booking) {
        return new BookingDto(
                booking.getId().value(),
                booking.getUserId().value(),
                booking.getRouteId().value(),
                booking.getSeatId().value(),
                booking.getStatus().name(),
                booking.getTotalPrice(),
                booking.getCurrency(),
                booking.getIdempotencyKey());
    }
}
