package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingUserInfo;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingUserInfoSummary;
import io.github.phunguy65.ttbs.backend.shared.domain.Money;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
class BookingEntityMapper {

    Booking toDomain(BookingEntity entity) {
        Money totalPrice = Money.of(
                BigDecimal.valueOf(entity.getTotalPrice()),
                Currency.getInstance(entity.getCurrency()));
        BookingUserInfoSnapshotJson snapshot = entity.getUserInfoSnapshot();
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
        entity.setUserInfoSnapshot(toSnapshot(domain.getUserInfo()));
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
}
