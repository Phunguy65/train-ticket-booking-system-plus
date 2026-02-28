package io.github.phunguy65.ttbs.backend.booking.application.service;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.station.domain.model.StationId;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private static final BigDecimal BASE_PRICE = new BigDecimal("500000");

    private PricingService pricingService;
    private Route route;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();

        route = Route.reconstitute(
                RouteId.of(UUID.randomUUID()),
                TrainId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                StationId.of(UUID.randomUUID()),
                Instant.now(),
                Instant.now().plus(2, ChronoUnit.HOURS),
                BASE_PRICE,
                RouteStatus.SCHEDULED,
                Instant.now());
    }

    private static Seat seatOf(String seatNumber) {
        return Seat.reconstitute(
                SeatId.of(UUID.randomUUID()),
                CoachId.of(UUID.randomUUID()),
                seatNumber,
                Instant.now(),
                null);
    }

    @Test
    void calculatePrices_shouldReturnFlatBasePriceForAllSeats() {
        Seat seat = seatOf("1A");

        List<BookedSeat> result = pricingService.calculatePrices(route, List.of(seat));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().unitPrice()).isEqualByComparingTo(new BigDecimal("500000.00"));
    }

    @Test
    void calculatePrices_multipleSeats_allGetSameBasePrice() {
        Seat seat1 = seatOf("1A");
        Seat seat2 = seatOf("1B");
        Seat seat3 = seatOf("1C");

        List<BookedSeat> result =
                pricingService.calculatePrices(route, List.of(seat1, seat2, seat3));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).unitPrice()).isEqualByComparingTo("500000.00");
        assertThat(result.get(1).unitPrice()).isEqualByComparingTo("500000.00");
        assertThat(result.get(2).unitPrice()).isEqualByComparingTo("500000.00");
    }

    @Test
    void calculateTotalPrice_twoSeats_shouldSumBothUnitPrices() {
        Seat seat1 = seatOf("1A");
        Seat seat2 = seatOf("1B");

        List<BookedSeat> bookedSeats = pricingService.calculatePrices(route, List.of(seat1, seat2));
        BigDecimal total = pricingService.calculateTotalPrice(bookedSeats);

        // 500000 + 500000 = 1000000
        assertThat(total).isEqualByComparingTo(new BigDecimal("1000000.00"));
    }

    @Test
    void calculateTotalPrice_emptySeatList_shouldReturnZero() {
        BigDecimal total = pricingService.calculateTotalPrice(List.of());
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
