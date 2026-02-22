package io.github.phunguy65.ttbs.backend.booking.domain.model;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.booking.domain.errors.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROUTE_ID = UUID.randomUUID();
    private static final UUID SEAT_ID = UUID.randomUUID();
    private static final BigDecimal PRICE = new BigDecimal("150000.00");
    private static final String CURRENCY = "VND";
    private static final String IDEMPOTENCY_KEY = "test-key-123";

    @Test
    void create_shouldStartInPendingStatus() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void create_shouldRegisterBookingCreatedEvent() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);

        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingCreated.class);
    }

    @Test
    void create_shouldSetCorrectFields() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);

        assertThat(booking.getUserId().value()).isEqualTo(USER_ID);
        assertThat(booking.getRouteId().value()).isEqualTo(ROUTE_ID);
        assertThat(booking.getSeatId().value()).isEqualTo(SEAT_ID);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo(PRICE);
        assertThat(booking.getCurrency()).isEqualTo(CURRENCY);
        assertThat(booking.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(booking.getId()).isNotNull();
    }

    @Test
    void confirm_fromPending_shouldTransitionToConfirmed() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingConfirmed.class);
    }

    @Test
    void confirm_fromConfirmed_shouldReturnCannotConfirmError() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);
        booking.confirm();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isFailure()).isTrue();
        assertThat(result).isInstanceOf(Result.Failure.class);
        BookingError error = ((Result.Failure<Void, BookingError>) result).error();
        assertThat(error).isInstanceOf(BookingError.CannotConfirm.class);
        assertThat(error.message()).contains("CONFIRMED");
    }

    @Test
    void confirm_fromCancelled_shouldReturnCannotConfirmError() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);
        booking.cancel();

        Result<Void, BookingError> result = booking.confirm();

        assertThat(result.isFailure()).isTrue();
        BookingError error = ((Result.Failure<Void, BookingError>) result).error();
        assertThat(error).isInstanceOf(BookingError.CannotConfirm.class);
        assertThat(error.message()).contains("CANCELLED");
    }

    @Test
    void cancel_fromPending_shouldTransitionToCancelled() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);
        booking.clearDomainEvents();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isSuccess()).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getDomainEvents()).hasSize(1);
        assertThat(booking.getDomainEvents().getFirst()).isInstanceOf(BookingCancelled.class);
    }

    @Test
    void cancel_fromCancelled_shouldReturnAlreadyCancelledError() {
        Booking booking =
                Booking.create(USER_ID, ROUTE_ID, SEAT_ID, PRICE, CURRENCY, IDEMPOTENCY_KEY);
        booking.cancel();

        Result<Void, BookingError> result = booking.cancel();

        assertThat(result.isFailure()).isTrue();
        BookingError error = ((Result.Failure<Void, BookingError>) result).error();
        assertThat(error).isInstanceOf(BookingError.AlreadyCancelled.class);
        assertThat(error.message()).contains("CANCELLED");
    }

    @Test
    void reconstitute_shouldNotRegisterDomainEvents() {
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.reconstitute(
                bookingId,
                USER_ID,
                ROUTE_ID,
                SEAT_ID,
                PRICE,
                CURRENCY,
                IDEMPOTENCY_KEY,
                BookingStatus.CONFIRMED,
                Instant.now());

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
                SEAT_ID,
                PRICE,
                CURRENCY,
                IDEMPOTENCY_KEY,
                BookingStatus.CONFIRMED,
                createdAt);

        assertThat(booking.getId().value()).isEqualTo(bookingId);
        assertThat(booking.getUserId().value()).isEqualTo(USER_ID);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getCreatedAt()).isEqualTo(createdAt);
    }
}
