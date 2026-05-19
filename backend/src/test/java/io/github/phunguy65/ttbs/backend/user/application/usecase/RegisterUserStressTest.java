package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
@DisplayName("RegisterUserUseCase stress")
class RegisterUserStressTest {

    private static final String EMAIL_PREFIX = "stress-register-";

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupStressUsers();
    }

    @AfterEach
    void tearDown() {
        cleanupStressUsers();
    }

    @Test
    @DisplayName("allows exactly one success when 50 threads register the same email")
    void execute_allowsExactlyOneSuccessForFiftyConcurrentDuplicateRegistrations()
            throws Exception {
        String email = EMAIL_PREFIX + "same@example.com";

        List<Result<UserResponse, UserError>> results = runConcurrentRegistrations(50, email);

        assertThat(results).hasSize(50);
        assertThat(results.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(results.stream().filter(Result::isFailure).count()).isEqualTo(49);
        assertThat(results.stream()
                        .filter(Result::isFailure)
                        .map(result -> ((Result.Failure<UserResponse, UserError>) result).error()))
                .allMatch(UserError.EmailAlreadyExists.class::isInstance);
        assertThat(countUsersByEmail(email)).isEqualTo(1);
    }

    @Test
    @DisplayName("succeeds for 100 sequential unique registrations")
    void execute_succeedsForOneHundredSequentialUniqueRegistrations() {
        List<Result<UserResponse, UserError>> results = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            String email = EMAIL_PREFIX + "unique-" + i + "@example.com";
            results.add(registerUserUseCase.execute(
                    new RegisterUserCommand(email, "secret123", "Sequential User " + i)));
        }

        assertThat(results).allMatch(Result::isSuccess);
        assertThat(countUsersByPrefix()).isEqualTo(100);
    }

    @Test
    @DisplayName("prevents duplicate rows when two threads race on the same email")
    void execute_preventsDuplicateRowsForTwoThreadRace() throws Exception {
        String email = EMAIL_PREFIX + "race@example.com";

        List<Result<UserResponse, UserError>> results = runConcurrentRegistrations(2, email);

        assertThat(results.stream().filter(Result::isSuccess).count()).isEqualTo(1);
        assertThat(results.stream().filter(Result::isFailure).count()).isEqualTo(1);
        assertThat(results.stream()
                        .filter(Result::isFailure)
                        .map(result -> ((Result.Failure<UserResponse, UserError>) result).error()))
                .allMatch(UserError.EmailAlreadyExists.class::isInstance);
        assertThat(countUsersByEmail(email)).isEqualTo(1);
    }

    private List<Result<UserResponse, UserError>> runConcurrentRegistrations(
            int threadCount, String email) throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<UserResponse, UserError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                int index = i;
                futures.add(executor.submit(registerTask(email, ready, start, index)));
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<Result<UserResponse, UserError>> results = new ArrayList<>();
            for (Future<Result<UserResponse, UserError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<Result<UserResponse, UserError>> registerTask(
            String email, CountDownLatch ready, CountDownLatch start, int index) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return registerUserUseCase.execute(
                    new RegisterUserCommand(email, "secret123", "Concurrent User " + index));
        };
    }

    private int countUsersByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ? AND deleted_at IS NULL",
                Integer.class,
                email);
        return count == null ? 0 : count;
    }

    private int countUsersByPrefix() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email LIKE ? AND deleted_at IS NULL",
                Integer.class,
                EMAIL_PREFIX + "%");
        return count == null ? 0 : count;
    }

    private void cleanupStressUsers() {
        jdbcTemplate.update("DELETE FROM users WHERE email LIKE ?", EMAIL_PREFIX + "%");
    }
}
