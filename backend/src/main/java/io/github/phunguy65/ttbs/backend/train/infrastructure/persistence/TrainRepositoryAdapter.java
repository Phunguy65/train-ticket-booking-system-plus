package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.TrainRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class TrainRepositoryAdapter implements TrainRepository {

    private final TrainJpaRepository jpaRepository;
    private final TrainEntityMapper mapper;

    TrainRepositoryAdapter(TrainJpaRepository jpaRepository, TrainEntityMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Train save(Train train) {
        TrainEntity entity = mapper.toEntity(train);
        TrainEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Train> findById(TrainId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public PageResult<Train> findAll(
            int page, int size, String sortField, SortDirection direction) {
        Sort.Direction sortDir =
                direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField));
        Slice<TrainEntity> slice = jpaRepository.findAll(pageable);
        List<Train> items = slice.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(items, page, size, slice.hasNext());
    }

    @Override
    public boolean existsByTrainNumber(String trainNumber) {
        return jpaRepository.existsByTrainNumber(trainNumber);
    }
}
