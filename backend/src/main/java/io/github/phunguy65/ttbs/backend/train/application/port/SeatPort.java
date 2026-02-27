package io.github.phunguy65.ttbs.backend.train.application.port;

import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import java.util.Optional;

/**
 * Cross-module port that allows the {@code booking} module to look up seat data.
 *
 * <p>Exposed via the {@code train::port} named interface — only seat data needed
 * for booking (e.g. seat class) is surfaced here.
 */
public interface SeatPort {

    /**
     * Finds a seat by its identifier.
     *
     * @param seatId the seat to look up
     * @return the seat if found
     */
    Optional<Seat> findById(SeatId seatId);
}
