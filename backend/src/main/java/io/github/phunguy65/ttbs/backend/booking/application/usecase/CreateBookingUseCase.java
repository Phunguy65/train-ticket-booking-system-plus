package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.application.dto.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
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

    public CreateBookingUseCase(
            BookingRepository bookingRepository, ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BookingDto execute(CreateBookingCommand command) {
        Optional<Booking> existing =
                bookingRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            return toDto(existing.get());
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

        return toDto(saved);
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
