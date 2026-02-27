package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.dto.HoldDto;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetBookingUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    private GetBookingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetBookingUseCase(bookingRepository);
    }

    private Booking buildHeldBooking(UUID bookingId, UUID userId, UUID routeId) {
        List<BookedSeat> seats =
                List.of(BookedSeat.of(SeatId.of(UUID.randomUUID()), BigDecimal.valueOf(100_000)));
        return Booking.reconstitute(
                bookingId,
                userId,
                routeId,
                seats,
                BigDecimal.valueOf(100_000),
                "VND",
                "key-001",
                BookingStatus.HELD,
                Instant.now(),
                Instant.now().plusSeconds(900),
                null,
                "Nguyen Van A",
                "a@example.com",
                null);
    }

    @Test
    void execute_withExistingId_shouldReturnPresentOptional() {
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        Booking booking = buildHeldBooking(bookingId, userId, routeId);
        when(bookingRepository.findByIdWithSeats(BookingId.of(bookingId)))
                .thenReturn(Optional.of(booking));

        Optional<HoldDto> result = useCase.execute(bookingId);

        assertThat(result).isPresent();
        assertThat(result.get().bookingId()).isEqualTo(bookingId);
        assertThat(result.get().status()).isEqualTo(BookingStatus.HELD.name());
        assertThat(result.get().seats()).hasSize(1);
    }

    @Test
    void execute_withUnknownId_shouldReturnEmptyOptional() {
        UUID unknownId = UUID.randomUUID();
        when(bookingRepository.findByIdWithSeats(BookingId.of(unknownId)))
                .thenReturn(Optional.empty());

        Optional<HoldDto> result = useCase.execute(unknownId);

        assertThat(result).isEmpty();
    }
}
