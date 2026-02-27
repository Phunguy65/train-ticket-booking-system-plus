package io.github.phunguy65.ttbs.backend.booking.application.service;

import io.github.phunguy65.ttbs.backend.booking.domain.model.BookedSeat;
import io.github.phunguy65.ttbs.backend.train.domain.model.Route;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Domain service responsible for calculating seat prices at hold creation time.
 *
 * <p>Formula: {@code unitPrice = route.basePrice} (flat — no per-seat-class multiplier).
 *
 * <p>The calculated prices are snapshots — they are stored in {@code booking_seats}
 * at hold time and never recalculated at confirmation.
 */
@Service
public class PricingService {

    /**
     * Calculates the unit price for each seat and returns a list of {@link BookedSeat} value
     * objects with price snapshots. All seats on the same route receive the same unit price
     * equal to the route's base price.
     *
     * @param route the route (provides the base price)
     * @param seats the seats to price
     * @return list of {@link BookedSeat} with snapshotted prices, in the same order as {@code seats}
     */
    public List<BookedSeat> calculatePrices(Route route, List<Seat> seats) {
        BigDecimal unitPrice = route.getBasePrice().setScale(2, RoundingMode.HALF_UP);
        return seats.stream()
                .map(seat -> BookedSeat.of(seat.getId(), unitPrice))
                .toList();
    }

    /**
     * Calculates the total price as the sum of all unit prices in the given list.
     *
     * @param bookedSeats the list of booked seats with pre-calculated unit prices
     * @return total price
     */
    public BigDecimal calculateTotalPrice(List<BookedSeat> bookedSeats) {
        return bookedSeats.stream()
                .map(BookedSeat::unitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
