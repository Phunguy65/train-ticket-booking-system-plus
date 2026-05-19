package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.SoftDeleteUserCommand;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
@DisplayName("DeleteAuthenticatedUserUseCase stress")
class DeleteAuthenticatedUserStressTest {

    private static final String EMAIL = "stress-delete@example.com";
    private static final String PASSWORD = "secret123";

    @Autowired
    private DeleteAuthenticatedUserUseCase deleteAuthenticatedUserUseCase;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupStressUsers();
        registerUserUseCase.execute(new RegisterUserCommand(EMAIL, PASSWORD, "Stress Delete User"));
    }

    @AfterEach
    void tearDown() {
        cleanupRefreshTokens();
        cleanupStressUsers();
    }

    @Test
    @DisplayName("allows 50 concurrent deletions of the same user with idempotent success")
    void execute_allowsFiftyConcurrentDeletionsOfSameUserWithIdempotentSuccess() throws Exception {
        UUID userId = findUserIdByEmail(EMAIL);

        List<Result<Void, UserError>> results = runConcurrentDeletes(50, userId);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(isUserDeleted(userId)).isTrue();
    }

    @Test
    @DisplayName("first deletion revokes tokens and subsequent ones are no-ops")
    void execute_firstDeletionRevokesTokensAndSubsequentOnesAreNoOps() throws Exception {
        UUID userId = findUserIdByEmail(EMAIL);
        insertRefreshToken(userId);

        List<Result<Void, UserError>> results = runConcurrentDeletes(50, userId);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(isTokenRevoked(userId)).isTrue();
    }

    private List<Result<Void, UserError>> runConcurrentDeletes(int threadCount, UUID userId)
            throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<Void, UserError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(deleteTask(userId, ready, start)));
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<Result<Void, UserError>> results = new ArrayList<>();
            for (Future<Result<Void, UserError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<Result<Void, UserError>> deleteTask(
            UUID userId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return deleteAuthenticatedUserUseCase.execute(
                    new SoftDeleteUserCommand(UserId.of(userId)));
        };
    }

    private UUID findUserIdByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    private boolean isUserDeleted(UUID userId) {
        Boolean deleted = jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM users WHERE id = ?", Boolean.class, userId);
        return Boolean.TRUE.equals(deleted);
    }

    private void cleanupStressUsers() {
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", EMAIL);
    }

    private void insertRefreshToken(UUID userId) {
        jdbcTemplate.update(
                "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at, revoked_at, created_at) VALUES (?, ?, ?, ?, NULL, CURRENT_TIMESTAMP)",
                UUID.randomUUID(),
                userId,
                "fake-hash-for-stress-test",
                Timestamp.from(Instant.now().plusSeconds(3600)));
    }

    private boolean isTokenRevoked(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ? AND revoked_at IS NULL",
                Integer.class,
                userId);
        return count != null && count == 0;
    }

    private void cleanupRefreshTokens() {
        jdbcTemplate.update(
                "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email = ?)",
                EMAIL);
    }
}
