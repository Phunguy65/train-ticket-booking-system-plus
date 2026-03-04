package io.github.phunguy65.ttbs.backend.booking.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingHeld;
import io.github.phunguy65.ttbs.backend.booking.domain.event.SeatHoldExpired;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID_1 = UUID.randomUUID();
    private static final UUID SEAT_ID_2 = UUID.randomUUID();
    private static final BigDecimal PRICE = new BigDecimal("150000.00");
    private static final String CURRENCY = "VND";
    private static final String IDEMPOTENCY_KEY = "test-key-123";
    private static final String PASSENGER_NAME = "Nguyen Van A";
    private static final String PASSENGER_EMAIL = "test@example.com";
    private static final String PASSENGER_PHONE = "+84901234567";
    private static final String SESSION_ID = "cs_test_abc123";

    private static List<BookedSeat> twoSeats() {
        return List.of(
                BookedSeat.of(SeatId.of(SEAT_ID_1), new BigDecimal("150000.00")),
                BookedSeat.of(SeatId.of(SEAT_ID_2), new BigDecimal("225000.00")));
    }

    private static Booking createHold(String checkoutSessionId) {
        BigDecimal total = new BigDecimal("375000.00");
        return Booking.createHold(
                USER_ID,
                ROUTE_ID,
                twoSeats(),
                total,
                CURRENCY,
                checkoutSessionId,
                IDEMPOTENCY_KEY,
                PASSENGER_NAME,
                PASSENGER_EMAIL,
                PASSENGER_PHONE);
    }

    private static Booking createHold() {
        return createHold(SESSION_ID);
    }

    // ── createHold() ─────────────────────────────────────────────────────────

    @Test
    void createHold_shouldStartInHeldStatus() {
        Booking booking = createHold();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.HELD);
    }

    @Test
    void createHold_shouldRegisterBookingHeldEvent() {
        Booking booking = createHold();

        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingHeld.class);
    }

    @Test
    void createHold_shouldSetCheckoutSessionId() {
        Booking booking = createHold(SESSION_ID);
        assertThat(booking.getCheckoutSessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    void createHold_withNullSessionId_shouldAllowNullCheckoutSessionId() {
        Booking booking = createHold(null);
        assertThat(booking.getCheckoutSessionId()).isNull();
    }

    @Test
    void createHold_shouldSetCorrectFields() {
        Booking booking = createHold();

        assertThat(booking.getUserId().value()).isEqualTo(USER_ID);
        assertThat(booking.getRouteId().value()).isEqualTo(ROUTE_ID);
        assertThat(booking.getBookedSeats()).hasSize(2);
        assertThat(booking.getCurrency()).isEqualTo(CURRENCY);
        assertThat(booking.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(booking.getId()).isNotNull();
    }

    // ── confirm() ────────────────────────────────────────────────────────────

    @Test
    void confirm_fromHeld_shouldTransitionToConfirmed() {
        Booking booking = createHold();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingConfirmed.class);
    }

    @Test
    void confirm_fromConfirmed_shouldReturnInvalidStatusTransition() {
        Booking booking = createHold();
        booking.confirm();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isFailure()).isTrue();
        BookingError error = ((Result.Failure<Void, BookingError>) result).error();
        assertThat(error).isInstanceOf(BookingError.InvalidStatusTransition.class);
        assertThat(error.message()).contains("CONFIRMED");
    }

    @Test
    void confirm_fromCancelled_shouldReturnInvalidStatusTransition() {
        Booking booking = createHold();
        booking.cancel();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isFailure()).isTrue();
        BookingError error = ((Result.Failure<Void, BookingError>) result).error();
        assertThat(error).isInstanceOf(BookingError.InvalidStatusTransition.class);
        assertThat(error.message()).contains("CANCELLED");
    }

    // ── expire() ─────────────────────────────────────────────────────────────

    @Test
    void expire_fromHeld_shouldTransitionToCancelled() {
        Booking booking = createHold();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.expire();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(SeatHoldExpired.class);
    }

    @Test
    void expire_fromCancelled_shouldReturnSuccessIdempotent() {
        Booking booking = createHold();
        booking.cancel();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.expire();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getDomainEvents()).isEmpty();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    // ── cancel() ─────────────────────────────────────────────────────────────

    @Test
    void cancel_fromHeld_shouldTransitionToCancelled() {
        Booking booking = createHold();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingCancelled.class);
    }

    @Test
    void cancel_fromHeld_bookingCancelledEvent_shouldCarryCheckoutSessionId() {
        Booking booking = createHold(SESSION_ID);
        booking.clearDomainEvents();
        booking.cancel();

        BookingCancelled event = (BookingCancelled) booking.getDomainEvents().getFirst();
        assertThat(event.checkoutSessionId()).isEqualTo(SESSION_ID);
        assertThat(event.bookingId()).isEqualTo(booking.getId());
    }

    @Test
    void cancel_fromConfirmed_shouldTransitionToCancelled() {
        Booking booking = createHold();
        booking.confirm();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancel_fromCancelled_shouldReturnAlreadyCancelled() {
        Booking booking = createHold();
        booking.cancel();
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isFailure()).isTrue();
        assertThat(((Result.Failure<Void, BookingError>) result).error())
                .isInstanceOf(BookingError.AlreadyCancelled.class);
        assertThat(booking.getDomainEvents()).isEmpty();
    }

    // ── reconstitute() ───────────────────────────────────────────────────────

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.reconstitute(
                bookingId,
                USER_ID,
                ROUTE_ID,
                twoSeats(),
                PRICE,
                CURRENCY,
                IDEMPOTENCY_KEY,
                BookingStatus.CONFIRMED,
                Instant.now(),
                SESSION_ID,
                PASSENGER_NAME,
                PASSENGER_EMAIL,
                PASSENGER_PHONE);

        assertThat(booking.getDomainEvents()).isEmpty();
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        UUID bookingId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Booking booking = Booking.reconstitute(
                bookingId,
                USER_ID,
                ROUTE_ID,
                twoSeats(),
                PRICE,
                CURRENCY,
                IDEMPOTENCY_KEY,
                BookingStatus.CONFIRMED,
                createdAt,
                SESSION_ID,
                PASSENGER_NAME,
                PASSENGER_EMAIL,
                PASSENGER_PHONE);

        assertThat(booking.getId().value()).isEqualTo(bookingId);
        assertThat(booking.getUserId().value()).isEqualTo(USER_ID);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getCreatedAt()).isEqualTo(createdAt);
        assertThat(booking.getCheckoutSessionId()).isEqualTo(SESSION_ID);
        assertThat(booking.getBookedSeats()).hasSize(2);
    }
}
