package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.application.dto.CreateBookingCommand;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateBookingUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateBookingUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final String IDEMPOTENCY_KEY = "idempotency-key-001";

    @BeforeEach
    void setUp() {
        useCase = new CreateBookingUseCase(bookingRepository, eventPublisher);
    }

    @Test
    void execute_shouldCreateAndPersistBooking() {
        CreateBookingCommand command =
                new CreateBookingCommand(USER_ID, ROUTE_ID, SEAT_ID, IDEMPOTENCY_KEY);
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = useCase.execute(command);

        verify(bookingRepository).save(any(Booking.class));
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.routeId()).isEqualTo(ROUTE_ID);
        assertThat(result.seatId()).isEqualTo(SEAT_ID);
        assertThat(result.status()).isEqualTo(BookingStatus.PENDING.name());
        assertThat(result.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void execute_withExistingIdempotencyKey_shouldReturnExistingBooking() {
        CreateBookingCommand command =
                new CreateBookingCommand(USER_ID, ROUTE_ID, SEAT_ID, IDEMPOTENCY_KEY);
        Booking existingBooking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, BigDecimal.ZERO, "VND", IDEMPOTENCY_KEY);
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(existingBooking));

        BookingDto result = useCase.execute(command);

        verify(bookingRepository, never()).save(any());
        assertThat(result.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void execute_shouldReturnDtoWithPendingStatus() {
        CreateBookingCommand command =
                new CreateBookingCommand(USER_ID, ROUTE_ID, SEAT_ID, IDEMPOTENCY_KEY);
        when(bookingRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingDto result = useCase.execute(command);

        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.id()).isNotNull();
    }
}
