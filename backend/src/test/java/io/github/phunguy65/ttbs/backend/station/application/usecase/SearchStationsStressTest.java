package io.github.phunguy65.ttbs.backend.station.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.station.application.query.GetStationByIdQuery;
import io.github.phunguy65.ttbs.backend.station.application.query.SearchStationsQuery;
import io.github.phunguy65.ttbs.backend.station.application.response.StationResponse;
import io.github.phunguy65.ttbs.backend.station.application.response.StationSearchResponse;
import io.github.phunguy65.ttbs.backend.station.domain.error.StationError;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfiguration.class)
@DisplayName("SearchStationsUseCase stress")
class SearchStationsStressTest {

    @Autowired
    private SearchStationsUseCase searchStationsUseCase;

    @Autowired
    private GetStationByIdUseCase getStationByIdUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupTestStations();
        insertStation("TESTHN", "Ha Noi", "Ha Noi");
        insertStation("TESTHT", "Ha Tinh", "Ha Tinh");
        insertStation("TESTSG", "Sai Gon", "Ho Chi Minh");
        insertStation("TESTDN", "Da Nang", "Da Nang");
    }

    @AfterEach
    void tearDown() {
        cleanupTestStations();
    }

    @Test
    @DisplayName("handles 50 concurrent requests with same keyword")
    void search_handles50ConcurrentRequestsWithSameKeyword() throws Exception {
        List<List<StationSearchResponse>> results =
                runConcurrentSearches(50, new SearchStationsQuery("Ha", 10));

        assertThat(results).hasSize(50);
        assertThat(results).allSatisfy(result -> assertThat(result).isEqualTo(results.getFirst()));
        assertThat(results.getFirst())
                .extracting(StationSearchResponse::name)
                .contains("Ha Noi", "Ha Tinh");
    }

    @Test
    @DisplayName("get by id handles 50 concurrent requests for same station")
    void getById_handles50ConcurrentRequestsForSameStation() throws Exception {
        UUID stationId = insertStation("TESTHP", "Hai Phong", "Hai Phong");

        List<Result<StationResponse, StationError>> results = runConcurrentGetById(50, stationId);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(results.stream()
                        .map(result ->
                                ((Result.Success<StationResponse, StationError>) result).value())
                        .toList())
                .allSatisfy(response -> {
                    assertThat(response.id()).isEqualTo(stationId);
                    assertThat(response.name()).isEqualTo("Hai Phong");
                });
    }

    private List<List<StationSearchResponse>> runConcurrentSearches(
            int threadCount, SearchStationsQuery query) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<List<StationSearchResponse>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(searchTask(query, ready, start)));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<List<StationSearchResponse>> results = new ArrayList<>();
            for (Future<List<StationSearchResponse>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private List<Result<StationResponse, StationError>> runConcurrentGetById(
            int threadCount, UUID stationId) throws Exception {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<StationResponse, StationError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(getByIdTask(stationId, ready, start)));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            List<Result<StationResponse, StationError>> results = new ArrayList<>();
            for (Future<Result<StationResponse, StationError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<List<StationSearchResponse>> searchTask(
            SearchStationsQuery query, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return searchStationsUseCase.execute(query);
        };
    }

    private Callable<Result<StationResponse, StationError>> getByIdTask(
            UUID stationId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return getStationByIdUseCase.execute(new GetStationByIdQuery(stationId));
        };
    }

    private UUID insertStation(String code, String name, String city) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO stations (id, code, name, city, created_at) VALUES (?, ?, ?, ?, ?)",
                id,
                code,
                name,
                city,
                Timestamp.from(Instant.now()));
        return id;
    }

    private void cleanupTestStations() {
        jdbcTemplate.update("DELETE FROM stations WHERE code LIKE 'TEST%'");
    }
}
