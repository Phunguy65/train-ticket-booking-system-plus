package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class SeatRepositoryAdapter implements SeatRepository {

    private final SeatJpaRepository jpaRepository;
    private final SeatEntityMapper mapper;

    SeatRepositoryAdapter(SeatJpaRepository jpaRepository, SeatEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Seat save(Seat seat) {
        SeatEntity entity = mapper.toEntity(seat);
        SeatEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Seat> findByCoachId(CoachId coachId) {
        return jpaRepository.findByCoachId(coachId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Seat> findById(SeatId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCoachIdAndSeatNumber(CoachId coachId, String seatNumber) {
        return jpaRepository.existsByCoachIdAndSeatNumber(coachId.value(), seatNumber);
    }

    @Override
    public void softDeleteById(SeatId id, Instant deletedAt) {
        jpaRepository.softDeleteById(id.value(), deletedAt);
    }

    @Override
    public int softDeleteByIds(List<SeatId> ids, Instant deletedAt) {
        List<UUID> uuids = ids.stream().map(SeatId::value).toList();
        return jpaRepository.softDeleteByIds(uuids, deletedAt);
    }
}
