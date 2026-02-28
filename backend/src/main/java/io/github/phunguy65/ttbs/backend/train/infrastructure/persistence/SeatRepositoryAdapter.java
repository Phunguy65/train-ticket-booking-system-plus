package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
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
    public List<Seat> findByTrainId(TrainId trainId) {
        return jpaRepository.findByTrainId(trainId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Seat> findById(SeatId id) {
        return jpaRepository.findActiveById(id.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTrainIdAndSeatNumber(TrainId trainId, String seatNumber) {
        return jpaRepository.existsByTrainIdAndSeatNumber(trainId.value(), seatNumber);
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
