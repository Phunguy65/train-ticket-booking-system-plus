package io.github.phunguy65.ttbs.backend.train.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RouteSeatAvailability")
class RouteSeatAvailabilityTest {

    private static final ScheduledTripId TRIP_ID =
            ScheduledTripId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final SeatId SEAT_ID =
            SeatId.of(UUID.fromString("00000000-0000-0000-0000-000000000002"));
    private static final UUID BOOKING_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Money PRICE_500K = Money.vnd(500_000L);

    // ─────────────────────────────────────────────────────────────
    // hold(UUID, Money)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hold(UUID, Money)")
    class HoldWithBookingIdAndPrice {

        @Test
        @DisplayName("AVAILABLE → HELD with bookingId and price")
        void available_toHeld_capturesBookingIdAndPrice() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            var result = seat.hold(BOOKING_ID, PRICE_500K);

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.HELD);
            assertThat(seat.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);
        }

        @Test
        @DisplayName("AVAILABLE → HELD with null price")
        void available_toHeld_capturesBookingIdWithNullPrice() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            var result = seat.hold(BOOKING_ID, null);

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.HELD);
            assertThat(seat.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(seat.getPriceAtBooking()).isNull();
        }

        @Test
        @DisplayName("HELD → fails with SeatNotAvailable")
        void held_toHeld_fails() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);

            var result = seat.hold(BOOKING_ID, PRICE_500K);

            assertThat(result.isFailure()).isTrue();
            assertThat(result)
                    .isInstanceOfSatisfying(
                            io.github.phunguy65.ttbs.backend.shared.domain.Result.Failure.class,
                            f -> assertThat(f.error())
                                    .isInstanceOf(
                                            RouteSeatAvailabilityError.SeatNotAvailable.class));
        }

        @Test
        @DisplayName("BOOKED → fails with SeatNotAvailable")
        void booked_toHeld_fails() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);
            seat.confirmHold();

            var result = seat.hold(BOOKING_ID, PRICE_500K);

            assertThat(result.isFailure()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // confirmHold()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmHold()")
    class ConfirmHold {

        @Test
        @DisplayName("HELD → BOOKED retains price snapshot")
        void held_toBooked_retainsPrice() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);

            var result = seat.confirmHold();

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
            assertThat(seat.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);
        }

        @Test
        @DisplayName("AVAILABLE → fails")
        void available_toBooked_fails() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            var result = seat.confirmHold();

            assertThat(result.isFailure()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // expire()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        @DisplayName("HELD → AVAILABLE clears bookingId and price")
        void held_toAvailable_clearsPrice() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);

            var result = seat.expire();

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
            assertThat(seat.getBookingId()).isNull();
            assertThat(seat.getPriceAtBooking()).isNull();
        }

        @Test
        @DisplayName("AVAILABLE → fails")
        void available_toExpire_fails() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            var result = seat.expire();

            assertThat(result.isFailure()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // cancel()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("BOOKED → CANCELLED retains price snapshot")
        void booked_toCancelled_retainsPrice() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);
            seat.confirmHold();

            var result = seat.cancel();

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.CANCELLED);
            assertThat(seat.getBookingId()).isNull();
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);
        }

        @Test
        @DisplayName("AVAILABLE → fails")
        void available_toCancelled_fails() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            var result = seat.cancel();

            assertThat(result.isFailure()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // release()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        @DisplayName("CANCELLED → AVAILABLE clears price")
        void cancelled_toAvailable_clearsPrice() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);
            seat.confirmHold();
            seat.cancel();

            var result = seat.release();

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
            assertThat(seat.getPriceAtBooking()).isNull();
        }

        @Test
        @DisplayName("BOOKED → fails")
        void booked_toRelease_fails() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);
            seat.confirmHold();

            var result = seat.release();

            assertThat(result.isFailure()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // book(Money)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("book(Money)")
    class BookWithPrice {

        @Test
        @DisplayName("AVAILABLE → BOOKED captures price")
        void available_toBooked_capturesPrice() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            var result = seat.book(PRICE_500K);

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);
            assertThat(seat.getBookingId()).isNull();
        }

        @Test
        @DisplayName("HELD → fails")
        void held_toBooked_fails() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);
            seat.hold(BOOKING_ID, PRICE_500K);

            var result = seat.book(PRICE_500K);

            assertThat(result.isFailure()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // hold() — parameterless overload
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hold() — parameterless")
    class HoldParameterless {

        @Test
        @DisplayName("AVAILABLE → HELD with null bookingId and null price")
        void available_toHeld_nullIds() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            var result = seat.hold();

            assertThat(result.isSuccess()).isTrue();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.HELD);
            assertThat(seat.getBookingId()).isNull();
            assertThat(seat.getPriceAtBooking()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Full lifecycle
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Full lifecycle: hold → confirm → cancel → release")
    class FullLifecycle {

        @Test
        @DisplayName(
                "AVAILABLE → HELD (price captured) → BOOKED (price retained) → CANCELLED (price retained) → AVAILABLE (price cleared)")
        void fullLifecycle_priceCaptureRetainClear() {
            RouteSeatAvailability seat = RouteSeatAvailability.create(TRIP_ID, SEAT_ID);

            // AVAILABLE → HELD: price captured
            seat.hold(BOOKING_ID, PRICE_500K);
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.HELD);
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);

            // HELD → BOOKED: price retained
            seat.confirmHold();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);

            // BOOKED → CANCELLED: price retained
            seat.cancel();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.CANCELLED);
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);

            // CANCELLED → AVAILABLE: price cleared
            seat.release();
            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.AVAILABLE);
            assertThat(seat.getPriceAtBooking()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // reconstitute() with price
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reconstitute(..., Money, Integer)")
    class ReconstituteWithPrice {

        @Test
        @DisplayName("reconstitutes with price snapshot")
        void reconstitute_withPrice() {
            var seat = RouteSeatAvailability.reconstitute(
                    TRIP_ID,
                    SEAT_ID,
                    RouteSeatAvailabilityStatus.BOOKED,
                    BOOKING_ID,
                    PRICE_500K,
                    5);

            assertThat(seat.getStatus()).isEqualTo(RouteSeatAvailabilityStatus.BOOKED);
            assertThat(seat.getBookingId()).isEqualTo(BOOKING_ID);
            assertThat(seat.getPriceAtBooking()).isEqualTo(PRICE_500K);
            assertThat(seat.getVersion()).isEqualTo(5);
        }
    }
}
