package io.github.phunguy65.ttbs.backend.booking.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.booking.domain.model.Booking;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingId;
import io.github.phunguy65.ttbs.backend.booking.domain.model.BookingStatus;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.projection.BookingUserInfoSummary;
import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.train.domain.model.ScheduledTripId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class BookingRepositoryAdapter implements BookingRepository {

    private final BookingJpaRepository jpaRepository;
    private final BookingEntityMapper mapper;

    BookingRepositoryAdapter(BookingJpaRepository jpaRepository, BookingEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Booking save(Booking booking) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(booking)));
    }

    @Override
    public Optional<Booking> findById(BookingId bookingId) {
        return jpaRepository.findById(bookingId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<BookingSummary> findSummaryById(BookingId bookingId) {
        return jpaRepository.findSummaryById(bookingId.value()).map(this::toSummary);
    }

    @Override
    public Optional<Booking> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Optional<Booking> findActiveHoldByUserAndScheduledTrip(
            UserId userId, ScheduledTripId scheduledTripId) {
        return jpaRepository
                .findByUserIdAndScheduledTripIdAndStatusHeld(
                        userId.value(), scheduledTripId.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Booking> findExpiredHeldBookings(Instant now) {
        return jpaRepository.findByStatusHeldAndPaymentDeadlineBefore(now).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<CancellationCandidate> findCancellationCandidatesByIds(List<BookingId> bookingIds) {
        List<java.util.UUID> uuids = bookingIds.stream().map(BookingId::value).toList();
        return jpaRepository.findCancellationCandidatesByIds(uuids).stream()
                .map(candidate -> new CancellationCandidate(
                        BookingId.of(candidate.getId()),
                        BookingStatus.valueOf(candidate.getStatus())))
                .toList();
    }

    @Override
    public void cancelByIds(List<BookingId> bookingIds) {
        List<java.util.UUID> uuids = bookingIds.stream().map(BookingId::value).toList();
        jpaRepository.cancelByIds(uuids);
    }

    @Override
    public List<Booking> saveAll(List<Booking> bookings) {
        List<BookingEntity> entities = bookings.stream().map(mapper::toEntity).toList();
        return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsActiveByUserId(UserId userId) {
        return jpaRepository.existsByUserIdAndStatusIn(
                userId.value(), List.of(BookingStatus.HELD.name(), BookingStatus.CONFIRMED.name()));
    }

    private BookingSummary toSummary(BookingSummaryView view) {
        BookingUserInfoSnapshotJson userInfo = view.getUserInfoSnapshot();
        return new BookingSummary(
                view.getId(),
                view.getUserId(),
                view.getScheduledTripId(),
                new BookingUserInfoSummary(
                        userInfo.fullName(),
                        userInfo.email(),
                        userInfo.phone(),
                        userInfo.dateOfBirth(),
                        userInfo.gender(),
                        userInfo.idDocumentNumber(),
                        userInfo.addressLine()),
                view.getTotalPrice(),
                view.getCurrency(),
                view.getStatus(),
                view.getPaymentDeadline(),
                view.getCreatedAt());
    }
}
