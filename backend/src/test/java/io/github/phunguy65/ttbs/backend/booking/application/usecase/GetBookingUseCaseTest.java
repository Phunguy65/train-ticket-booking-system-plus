package io.github.phunguy65.ttbs.backend.booking.application.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.ttbs.backend.booking.application.dto.BookingDto;
import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
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

@ExtendWith(MockitoExtension.class)
class GetBookingUseCaseTest {

    @Mock
    private BookingRepository bookingRepository;

    private GetBookingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetBookingUseCase(bookingRepository);
    }

    @Test
    void execute_withExistingId_shouldReturnPresentOptional() {
        UUID userId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        Booking booking =
                Booking.create(userId, routeId, seatId, new BigDecimal("100.00"), "VND", "key-001");
        UUID bookingId = booking.getId().value();
        when(bookingRepository.findById(BookingId.of(bookingId))).thenReturn(Optional.of(booking));

        Optional<BookingDto> result = useCase.execute(bookingId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(bookingId);
        assertThat(result.get().userId()).isEqualTo(userId);
        assertThat(result.get().status()).isEqualTo(BookingStatus.PENDING.name());
    }

    @Test
    void execute_withUnknownId_shouldReturnEmptyOptional() {
        UUID unknownId = UUID.randomUUID();
        when(bookingRepository.findById(BookingId.of(unknownId))).thenReturn(Optional.empty());

        Optional<BookingDto> result = useCase.execute(unknownId);

        assertThat(result).isEmpty();
    }
}
