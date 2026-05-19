package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingPassenger;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingPassengerSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingUserInfoSummary;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
class BookingEntityMapper {

    Booking toDomain(BookingEntity entity) {
        Money totalPrice = Money.of(
                BigDecimal.valueOf(entity.getTotalPrice()),
                Currency.getInstance(entity.getCurrency()));
        BookingUserInfoSnapshotJson snapshot = entity.getUserInfoSnapshot();

        // Map passengers from snapshot (null-safe for legacy rows)
        List<BookingPassenger> passengers = mapPassengersToDomain(entity.getPassengersSnapshot());

        return Booking.reconstitute(
                BookingId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                ScheduledTripId.of(entity.getScheduledTripId()),
                BookingUserInfo.of(
                        snapshot.fullName(),
                        snapshot.email(),
                        snapshot.phone(),
                        snapshot.dateOfBirth(),
                        snapshot.gender(),
                        snapshot.idDocumentNumber(),
                        snapshot.addressLine()),
                passengers,
                totalPrice,
                BookingStatus.valueOf(entity.getStatus()),
                entity.getIdempotencyKey(),
                entity.getPaymentDeadline(),
                entity.getCreatedAt());
    }

    BookingEntity toEntity(Booking domain) {
        BookingEntity entity = new BookingEntity();
        entity.setId(domain.getBookingId().value());
        entity.setUserId(domain.getUserId().value());
        entity.setScheduledTripId(domain.getScheduledTripId().value());
        entity.setUserInfoSnapshot(toSnapshot(domain.getBookerInfo()));
        entity.setPassengersSnapshot(toPassengersSnapshot(domain.getPassengers()));
        entity.setTotalPrice(domain.getTotalPrice().toLong());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(domain.getStatus().name());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setPaymentDeadline(domain.getPaymentDeadline());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    BookingSummary toSummary(BookingEntity entity) {
        BookingUserInfoSnapshotJson snapshot = Objects.requireNonNull(
                entity.getUserInfoSnapshot(), "Booking user info snapshot must not be null");

        List<BookingPassengerSummary> passengerSummaries =
                mapPassengersToSummary(entity.getPassengersSnapshot());

        return new BookingSummary(
                entity.getId(),
                entity.getUserId(),
                entity.getScheduledTripId(),
                new BookingUserInfoSummary(
                        snapshot.fullName(),
                        snapshot.email(),
                        snapshot.phone(),
                        snapshot.dateOfBirth(),
                        snapshot.gender(),
                        snapshot.idDocumentNumber(),
                        snapshot.addressLine()),
                passengerSummaries,
                entity.getTotalPrice(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getPaymentDeadline(),
                entity.getCreatedAt());
    }

    private BookingUserInfoSnapshotJson toSnapshot(BookingUserInfo userInfo) {
        return new BookingUserInfoSnapshotJson(
                userInfo.fullName(),
                userInfo.email(),
                userInfo.phone(),
                userInfo.dateOfBirth(),
                userInfo.gender(),
                userInfo.idDocumentNumber(),
                userInfo.addressLine());
    }

    private List<BookingPassengerSnapshotJson> toPassengersSnapshot(
            List<BookingPassenger> passengers) {
        if (passengers == null || passengers.isEmpty()) {
            return null;
        }
        return passengers.stream()
                .map(p -> new BookingPassengerSnapshotJson(
                        p.seatId().value(),
                        p.fullName(),
                        p.idDocumentNumber(),
                        p.dateOfBirth(),
                        p.gender()))
                .toList();
    }

    private List<BookingPassenger> mapPassengersToDomain(
            List<BookingPassengerSnapshotJson> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        return snapshots.stream()
                .map(s -> BookingPassenger.of(
                        SeatId.of(s.seatId()),
                        s.fullName(),
                        s.idDocumentNumber(),
                        s.dateOfBirth(),
                        s.gender()))
                .toList();
    }

    private List<BookingPassengerSummary> mapPassengersToSummary(
            List<BookingPassengerSnapshotJson> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        return snapshots.stream()
                .map(s -> new BookingPassengerSummary(
                        s.seatId(),
                        s.fullName(),
                        s.idDocumentNumber(),
                        s.dateOfBirth(),
                        s.gender()))
                .toList();
    }
}
