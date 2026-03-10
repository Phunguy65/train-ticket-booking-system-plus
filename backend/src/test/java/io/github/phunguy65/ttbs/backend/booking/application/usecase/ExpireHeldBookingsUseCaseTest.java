package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.application.port.RouteSeatAvailabilityPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ExpireHeldBookingsUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RouteSeatAvailabilityPort seatAvailabilityPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ExpireHeldBookingsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExpireHeldBookingsUseCase(
                bookingRepository, seatAvailabilityPort, eventPublisher);
    }

    private Booking expiredHeldBooking(UUID bookingUuid) {
        return Booking.reconstitute(
                BookingId.of(bookingUuid),
                UserId.of(UUID.randomUUID()),
                RouteId.of(UUID.randomUUID()),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.HELD,
                "idem-key-" + bookingUuid,
                Instant.now().minusSeconds(60), // already expired
                Instant.now().minusSeconds(1000));
    }

    @Test
    void execute_noExpired_shouldDoNothing() {
        when(bookingRepository.findExpiredHeldBookings(any())).thenReturn(List.of());

        useCase.execute();

        verify(bookingRepository, never()).saveAll(any());
        verify(seatAvailabilityPort, never()).releaseHeldSeats(any(), any());
    }

    @Test
    void execute_multipleExpired_shouldCancelAllAndReleaseSeats() {
        UUID b1 = UUID.randomUUID();
        UUID b2 = UUID.randomUUID();
        List<Booking> expired = List.of(expiredHeldBooking(b1), expiredHeldBooking(b2));
        when(bookingRepository.findExpiredHeldBookings(any())).thenReturn(expired);
        when(seatAvailabilityPort.findSeatIdsByBookingId(any()))
                .thenReturn(List.of(SeatId.of(UUID.randomUUID())));
        when(seatAvailabilityPort.releaseHeldSeats(any(), any()))
                .thenReturn(io.github.phunguy65.ttbs.backend.shared.domain.Result.success());
        when(bookingRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute();

        verify(bookingRepository).saveAll(argThat(list -> ((List<?>) list).size() == 2));
        verify(seatAvailabilityPort, times(2)).releaseHeldSeats(any(), any());
    }

    @Test
    void execute_idempotency_alreadyCancelledBooking_shouldSkip() {
        Booking alreadyCancelled = Booking.reconstitute(
                BookingId.of(UUID.randomUUID()),
                UserId.of(UUID.randomUUID()),
                RouteId.of(UUID.randomUUID()),
                "John Doe",
                "john@example.com",
                null,
                Money.vnd(100_000L),
                "VND",
                BookingStatus.CANCELLED,
                "idem-key",
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(1000));
        when(bookingRepository.findExpiredHeldBookings(any()))
                .thenReturn(List.of(alreadyCancelled));

        useCase.execute();

        // Already cancelled — cancel() returns failure, so it's skipped
        verify(bookingRepository, never()).saveAll(any());
    }
}
