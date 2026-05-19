package io.github.phunguy65.ttbs.backend.booking.domain.model;

import io.github.phunguy65.ttbs.backend.booking.domain.error.BookingError;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCancelled;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingConfirmed;
import io.github.phunguy65.ttbs.backend.booking.domain.event.BookingCreated;
import io.github.phunguy65.ttbs.backend.shared.domain.AggregateRoot;
import io.github.phunguy65.ttbs.backend.shared.domain.IdempotencyKey;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aggregate root representing a train ticket booking.
 *
 * <p>Status machine:
 * <ul>
 *   <li>HELD → CONFIRMED (via {@link #confirm()})
 *   <li>HELD → CANCELLED (via {@link #cancel()})
 *   <li>CONFIRMED → CANCELLED (via {@link #cancel()})
 * </ul>
 */
public class Booking extends AggregateRoot<BookingId> {

    private final BookingId bookingId;
    private final UserId userId;
    private final ScheduledTripId scheduledTripId;
    private final BookingUserInfo bookerInfo;
    private final List<BookingPassenger> passengers;
    private final Money totalPrice;
    private BookingStatus status;
    private final IdempotencyKey idempotencyKey;
    private final Instant paymentDeadline;
    private final Instant createdAt;

    private Booking(
            BookingId bookingId,
            UserId userId,
            ScheduledTripId scheduledTripId,
            BookingUserInfo bookerInfo,
            List<BookingPassenger> passengers,
            Money totalPrice,
            BookingStatus status,
            String idempotencyKey,
            Instant paymentDeadline,
            Instant createdAt) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.scheduledTripId = scheduledTripId;
        this.bookerInfo = bookerInfo;
        this.passengers = passengers == null ? List.of() : List.copyOf(passengers);
        this.totalPrice = totalPrice;
        this.status = status;
        this.idempotencyKey = IdempotencyKey.of(idempotencyKey);
        this.paymentDeadline = paymentDeadline;
        this.createdAt = createdAt;

        validateUniquePassengerIdDocuments(this.passengers);
    }

    /**
     * Validates that all passengers have unique ID document numbers.
     */
    private static void validateUniquePassengerIdDocuments(List<BookingPassenger> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            return;
        }
        Set<String> seenIds = new HashSet<>();
        for (BookingPassenger passenger : passengers) {
            String idDoc = passenger.idDocumentNumber();
            if (idDoc != null && !seenIds.add(idDoc)) {
                throw new IllegalArgumentException(
                        "Duplicate passenger ID document number: " + idDoc);
            }
        }
    }

    /**
     * Creates a new booking in HELD status and registers a {@link BookingCreated} event.
     */
    public static Booking create(
            BookingId bookingId,
            UserId userId,
            ScheduledTripId scheduledTripId,
            BookingUserInfo bookerInfo,
            List<BookingPassenger> passengers,
            Money totalPrice,
            String idempotencyKey,
            Instant paymentDeadline) {
        Booking booking = new Booking(
                bookingId,
                userId,
                scheduledTripId,
                bookerInfo,
                passengers,
                totalPrice,
                BookingStatus.HELD,
                idempotencyKey,
                paymentDeadline,
                Instant.now());
        booking.registerEvent(new BookingCreated(
                bookingId,
                userId,
                scheduledTripId,
                totalPrice,
                totalPrice.getCurrency().getCurrencyCode()));
        return booking;
    }

    /**
     * Reconstitutes a booking from persistence — no events registered.
     */
    public static Booking reconstitute(
            BookingId bookingId,
            UserId userId,
            ScheduledTripId scheduledTripId,
            BookingUserInfo bookerInfo,
            List<BookingPassenger> passengers,
            Money totalPrice,
            BookingStatus status,
            String idempotencyKey,
            Instant paymentDeadline,
            Instant createdAt) {
        return new Booking(
                bookingId,
                userId,
                scheduledTripId,
                bookerInfo,
                passengers,
                totalPrice,
                status,
                idempotencyKey,
                paymentDeadline,
                createdAt);
    }

    /**
     * Transitions status from HELD to CONFIRMED and registers a {@link BookingConfirmed} event.
     *
     * @return success if status was HELD; failure with {@link BookingError.InvalidStatusTransition} otherwise
     */
    public Result<Void, BookingError> confirm() {
        if (status != BookingStatus.HELD) {
            return Result.failure(new BookingError.InvalidStatusTransition(
                    status.name(), BookingStatus.CONFIRMED.name()));
        }
        this.status = BookingStatus.CONFIRMED;
        registerEvent(new BookingConfirmed(bookingId, userId, scheduledTripId));
        return Result.success();
    }

    /**
     * Transitions status from HELD or CONFIRMED to CANCELLED and registers a
     * {@link BookingCancelled} event. The event carries {@code requiresRefund=true} when
     * cancelling a CONFIRMED booking.
     *
     * @return success if status was HELD or CONFIRMED;
     *         failure with {@link BookingError.InvalidStatusTransition} if already CANCELLED
     */
    public Result<Void, BookingError> cancel() {
        if (status == BookingStatus.CANCELLED) {
            return Result.failure(new BookingError.InvalidStatusTransition(
                    status.name(), BookingStatus.CANCELLED.name()));
        }
        boolean requiresRefund = (status == BookingStatus.CONFIRMED);
        this.status = BookingStatus.CANCELLED;
        registerEvent(new BookingCancelled(bookingId, userId, scheduledTripId, requiresRefund));
        return Result.success();
    }

    @Override
    public BookingId getId() {
        return bookingId;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public UserId getUserId() {
        return userId;
    }

    public ScheduledTripId getScheduledTripId() {
        return scheduledTripId;
    }

    /**
     * Returns the booker (authenticated user) information snapshot.
     */
    public BookingUserInfo getBookerInfo() {
        return bookerInfo;
    }

    /**
     * Returns the list of passengers assigned to seats.
     * May be empty for legacy bookings.
     */
    public List<BookingPassenger> getPassengers() {
        return passengers;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public String getCurrency() {
        return totalPrice.getCurrency().getCurrencyCode();
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey.value();
    }

    public Instant getPaymentDeadline() {
        return paymentDeadline;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
