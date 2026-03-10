package io.github.phunguy65.ttbs.backend.train.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.train.domain.error.RouteSeatAvailabilityError;
import io.github.phunguy65.ttbs.backend.train.domain.model.Coach;
import io.github.phunguy65.ttbs.backend.train.domain.model.CoachId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteId;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailability;
import io.github.phunguy65.ttbs.backend.train.domain.model.RouteSeatAvailabilityStatus;
import io.github.phunguy65.ttbs.backend.train.domain.model.Seat;
import io.github.phunguy65.ttbs.backend.train.domain.model.SeatId;
import io.github.phunguy65.ttbs.backend.train.domain.model.Train;
import io.github.phunguy65.ttbs.backend.train.domain.model.TrainId;
import io.github.phunguy65.ttbs.backend.train.domain.repository.RouteSeatAvailabilityRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration test that validates optimistic locking ({@code @Version}) is the concurrency guard
 * for {@code route_seat_availability} rows.
 *
 * <p>Two threads race to hold the same seat. Exactly one must succeed; the other must receive an
 * {@code OptimisticLockException} (surfaced as a Spring data exception or a failure result).
 * No double-hold inconsistency must remain.
 *
 * <p>Test runs without an enclosing transaction ({@code NOT_SUPPORTED}) so that each thread can
 * commit its own independent transaction and the optimistic lock is exercised properly.
 */
@DataJpaTest
@Import({
    RouteSeatAvailabilityRepositoryAdapter.class,
    RouteSeatAvailabilityEntityMapper.class,
    SeatRepositoryAdapter.class,
    SeatEntityMapper.class,
    CoachRepositoryAdapter.class,
    CoachEntityMapper.class,
    TrainRepositoryAdapter.class,
    TrainEntityMapper.class
})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrentSeatHoldTest {

    @Autowired
    private RouteSeatAvailabilityRepository availabilityRepository;

    @Autowired
    private SeatRepositoryAdapter seatRepository;

    @Autowired
    private CoachRepositoryAdapter coachRepository;

    @Autowired
    private TrainRepositoryAdapter trainRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private RouteId routeId;
    private SeatId seatId;

    @BeforeEach
    void setUp() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            TrainId trainId = TrainId.of(UUID.randomUUID());
            trainRepository.save(Train.create(
                    trainId,
                    "CONC-TEST-" + trainId.value().toString().substring(0, 8),
                    "Concurrent Test",
                    100));

            CoachId coachId = CoachId.of(UUID.randomUUID());
            coachRepository.save(Coach.create(coachId, trainId, 1, 50));

            routeId = RouteId.of(UUID.randomUUID());

            Seat seat = Seat.create(SeatId.of(UUID.randomUUID()), coachId, "1A");
            seatId = seatRepository.save(seat).getId();

            availabilityRepository.saveAll(List.of(RouteSeatAvailability.create(routeId, seatId)));
            return null;
        });
    }

    /**
     * Two threads simultaneously attempt to hold the same seat.
     * Exactly one MUST succeed; the other MUST receive a failure (OptimisticLockException or
     * seat-unavailable result). After both threads complete, the seat MUST NOT be double-held.
     */
    @Test
    void concurrentHold_onSameSeat_exactlyOneSucceeds() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // Thread task: attempt holdSeats inside its own transaction
        Runnable holdTask = () -> {
            try {
                startGate.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            try {
                Result<Void, RouteSeatAvailabilityError> result = txTemplate.execute(status -> {
                    List<RouteSeatAvailability> seats =
                            availabilityRepository.findByRouteIdAndSeatIds(
                                    routeId, List.of(seatId));
                    if (seats.size() != 1) {
                        return Result.failure(new RouteSeatAvailabilityError.SeatNotAvailable());
                    }
                    RouteSeatAvailability seat = seats.getFirst();
                    Result<Void, RouteSeatAvailabilityError> holdResult = seat.hold();
                    if (holdResult.isFailure()) {
                        return holdResult;
                    }
                    availabilityRepository.save(seat);
                    return Result.success();
                });

                if (result != null && result.isSuccess()) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            } catch (Exception ex) {
                // OptimisticLockException or any concurrency exception counts as a failure
                // (expected)
                failureCount.incrementAndGet();
            }
        };

        Future<?> f1 = executor.submit(holdTask);
        Future<?> f2 = executor.submit(holdTask);

        // Release both threads simultaneously
        startGate.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // ── Assert: exactly one success, one failure ──────────────────────────
        assertThat(successCount.get())
                .as("Exactly one concurrent hold must succeed")
                .isEqualTo(1);
        assertThat(failureCount.get())
                .as("Exactly one concurrent hold must fail")
                .isEqualTo(1);

        // ── Assert: no double-hold — seat is in HELD state exactly once ───────
        TransactionTemplate readTx = new TransactionTemplate(transactionManager);
        readTx.execute(status -> {
            List<RouteSeatAvailability> all =
                    availabilityRepository.findByRouteIdAndSeatIds(routeId, List.of(seatId));
            assertThat(all).hasSize(1);
            assertThat(all.getFirst().getStatus())
                    .as("Seat must be HELD (not double-held or still AVAILABLE)")
                    .isEqualTo(RouteSeatAvailabilityStatus.HELD);
            return null;
        });
    }
}
