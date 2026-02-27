package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.command.CreateSeatHoldCommand;
import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.application.service.PricingService;
import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.application.port.RoutePort;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.application.port.SeatPort;
import io.github.phunguy65.ttbs.backend.train.domain.errors.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateSeatHoldUseCase {

    private static final String DEFAULT_CURRENCY = "VND";
    private static final int HOLD_DURATION_MINUTES = 15;

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RouteSeatAvailabilityPort seatAvailabilityPort;
    private final RoutePort routePort;
    private final SeatPort seatPort;
    private final PricingService pricingService;

    public CreateSeatHoldUseCase(
            BookingRepository bookingRepository,
            ApplicationEventPublisher eventPublisher,
            RouteSeatAvailabilityPort seatAvailabilityPort,
            RoutePort routePort,
            SeatPort seatPort,
            PricingService pricingService) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
        this.seatAvailabilityPort = seatAvailabilityPort;
        this.routePort = routePort;
        this.seatPort = seatPort;
        this.pricingService = pricingService;
    }

    @Transactional
    public Result<HoldDto, BookingError> execute(CreateSeatHoldCommand command) {
        Optional<Booking> existing =
                bookingRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return Result.success(toHoldDto(existing.get()));
        }

        boolean activeHoldExists = bookingRepository
                .findActiveHoldByUserIdAndRouteId(
                        UserId.of(command.userId()), RouteId.of(command.routeId()))
                .isPresent();
        if (activeHoldExists) {
            return Result.failure(new BookingError.ActiveHoldExists());
        }

        Route route = routePort
                .findById(RouteId.of(command.routeId()))
                .orElseThrow(() ->
                        new IllegalArgumentException("Route not found: " + command.routeId()));

        List<Seat> seats = loadSeats(command.seatIds());

        List<BookedSeat> bookedSeats = pricingService.calculatePrices(route, seats);
        var totalPrice = pricingService.calculateTotalPrice(bookedSeats);

        List<SeatId> seatIds = command.seatIds().stream().map(SeatId::of).toList();
        Result<Void, RouteSeatAvailabilityError> holdResult =
                seatAvailabilityPort.holdSeats(RouteId.of(command.routeId()), seatIds);

        if (holdResult.isFailure()) {
            return Result.failure(new BookingError.SeatsNotAvailable(command.seatIds()));
        }

        Instant paymentDeadline = Instant.now().plus(HOLD_DURATION_MINUTES, ChronoUnit.MINUTES);
        Booking booking = Booking.createHold(
                command.userId(),
                command.routeId(),
                bookedSeats,
                totalPrice,
                DEFAULT_CURRENCY,
                paymentDeadline,
                command.idempotencyKey(),
                command.passengerName(),
                command.passengerEmail(),
                command.passengerPhone());

        try {
            Booking saved = bookingRepository.save(booking);

            for (DomainEvent event : booking.getDomainEvents()) {
                eventPublisher.publishEvent(event);
            }
            booking.clearDomainEvents();

            return Result.success(toHoldDto(saved));
        } catch (DataIntegrityViolationException ex) {
            return Result.failure(new BookingError.ActiveHoldExists());
        }
    }

    private List<Seat> loadSeats(List<UUID> seatIds) {
        List<Seat> seats = new ArrayList<>();
        for (UUID seatId : seatIds) {
            Seat seat = seatPort.findById(SeatId.of(seatId))
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));
            seats.add(seat);
        }
        return seats;
    }

    private HoldDto toHoldDto(Booking booking) {
        List<HoldDto.BookedSeatDto> seatDtos = booking.getBookedSeats().stream()
                .map(bs -> new HoldDto.BookedSeatDto(bs.seatId().value(), bs.unitPrice()))
                .toList();
        return new HoldDto(
                booking.getId().value(),
                booking.getStatus().name(),
                booking.getRouteId().value(),
                seatDtos,
                booking.getTotalPrice(),
                booking.getCurrency(),
                booking.getPaymentDeadline());
    }
}
