package io.github.phunguy65.ttbs.backend.train.infrastructure.adapter;

import io.github.phunguy65.ttbs.backend.train.application.port.SeatPort;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.SeatRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing {@link SeatPort} — delegates to the internal {@link SeatRepository}.
 * Bridges the cross-module boundary while keeping JPA details inside the train module.
 */
@Component
public class SeatPortAdapter implements SeatPort {

    private final SeatRepository seatRepository;

    public SeatPortAdapter(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    public Optional<Seat> findById(SeatId seatId) {
        return seatRepository.findById(seatId);
    }
}
