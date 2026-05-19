package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RouteSeatAvailabilityManagerAdapter")
class RouteSeatAvailabilityManagerAdapterTest {

    private static final ScheduledTripId TRIP_ID =
            ScheduledTripId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final SeatId SEAT_1 =
            SeatId.of(UUID.fromString("00000000-0000-0000-0000-000000000011"));
    private static final SeatId SEAT_2 =
            SeatId.of(UUID.fromString("00000000-0000-0000-0000-000000000012"));
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Money PRICE = Money.vnd(500_000L);

    @Mock
    private RouteSeatAvailabilityRepository repository;

    private RouteSeatAvailabilityManagerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RouteSeatAvailabilityManagerAdapter(repository);
    }

    @Nested
    @DisplayName("holdSeatsWithBookingId(UUID, List<SeatId>, UUID, Money)")
    class HoldSeatsWithBookingId {

        @Test
        @DisplayName("AVAILABLE seats → HELD with bookingId and price snapshot")
        void available_seats_areHeld_withBookingIdAndPrice() {
            RouteSeatAvailability seat1 = RouteSeatAvailability.create(TRIP_ID, SEAT_1);
            RouteSeatAvailability seat2 = RouteSeatAvailability.create(TRIP_ID, SEAT_2);
            when(repository.findByScheduledTripIdAndSeatIds(TRIP_ID, List.of(SEAT_1, SEAT_2)))
                    .thenReturn(List.of(seat1, seat2));
            when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            var result = adapter.holdSeatsWithBookingId(
                    TRIP_ID, List.of(SEAT_1, SEAT_2), BOOKING_ID, PRICE);

            assertThat(result.isSuccess()).isTrue();

            ArgumentCaptor<List<RouteSeatAvailability>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());

            List<RouteSeatAvailability> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).allMatch(s -> s.getStatus() == RouteSeatAvailabilityStatus.HELD);
            assertThat(saved).allMatch(s -> s.getBookingId().equals(BOOKING_ID));
            assertThat(saved).allMatch(s -> s.getPriceAtBooking().equals(PRICE));
        }

        @Test
        @DisplayName("one seat unavailable → fails with SeatNotAvailable, no seats modified")
        void seatUnavailable_failsAndNoModification() {
            RouteSeatAvailability seat1 = RouteSeatAvailability.create(TRIP_ID, SEAT_1);
            RouteSeatAvailability seat2 = RouteSeatAvailability.create(TRIP_ID, SEAT_2);
            seat2.hold(UUID.randomUUID(), Money.vnd(300_000L));
            when(repository.findByScheduledTripIdAndSeatIds(TRIP_ID, List.of(SEAT_1, SEAT_2)))
                    .thenReturn(List.of(seat1, seat2));

            var result = adapter.holdSeatsWithBookingId(
                    TRIP_ID, List.of(SEAT_1, SEAT_2), BOOKING_ID, PRICE);

            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("seats not found → requested size not matched → fails")
        void seatsNotFound_requestedSizeNotMatched_fails() {
            RouteSeatAvailability seat1 = RouteSeatAvailability.create(TRIP_ID, SEAT_1);
            when(repository.findByScheduledTripIdAndSeatIds(TRIP_ID, List.of(SEAT_1, SEAT_2)))
                    .thenReturn(List.of(seat1));

            var result = adapter.holdSeatsWithBookingId(
                    TRIP_ID, List.of(SEAT_1, SEAT_2), BOOKING_ID, PRICE);

            assertThat(result.isFailure()).isTrue();
        }

        @Test
        @DisplayName("price snapshot is passed to each seat individually")
        void allSeatsReceiveSamePriceSnapshot() {
            RouteSeatAvailability seat1 = RouteSeatAvailability.create(TRIP_ID, SEAT_1);
            RouteSeatAvailability seat2 = RouteSeatAvailability.create(TRIP_ID, SEAT_2);
            when(repository.findByScheduledTripIdAndSeatIds(TRIP_ID, List.of(SEAT_1, SEAT_2)))
                    .thenReturn(List.of(seat1, seat2));
            when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            adapter.holdSeatsWithBookingId(TRIP_ID, List.of(SEAT_1, SEAT_2), BOOKING_ID, PRICE);

            ArgumentCaptor<List<RouteSeatAvailability>> captor =
                    ArgumentCaptor.forClass(List.class);
            verify(repository).saveAll(captor.capture());
            assertThat(captor.getValue()).allMatch(s -> s.getPriceAtBooking().equals(PRICE));
        }
    }
}
