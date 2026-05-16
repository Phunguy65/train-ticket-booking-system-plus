package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.LoginCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.LogoutUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse;
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
@DisplayName("LogoutUserUseCase stress")
class LogoutUserStressTest {

    private static final String EMAIL = "stress-logout@example.com";
    private static final String PASSWORD = "secret123";

    @Autowired
    private LogoutUserUseCase logoutUserUseCase;

    @Autowired
    private LoginUserUseCase loginUserUseCase;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupStressUser();
        registerUserUseCase.execute(new RegisterUserCommand(EMAIL, PASSWORD, "Stress Logout User"));
    }

    @AfterEach
    void tearDown() {
        cleanupStressUser();
    }

    @Test
    @DisplayName("allows 50 concurrent logouts with the same refresh token")
    void execute_allowsFiftyConcurrentLogoutsWithSameToken() throws Exception {
        String refreshToken = loginAndGetRefreshToken();

        List<Result<Void, UserError>> results = runConcurrentLogouts(copiesOf(refreshToken, 50));

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(countRevokedRefreshTokensForStressUser()).isEqualTo(1);
    }

    @Test
    @DisplayName("allows 50 concurrent logouts with different refresh tokens")
    void execute_allowsFiftyConcurrentLogoutsWithDifferentTokens() throws Exception {
        List<String> refreshTokens = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            refreshTokens.add(loginAndGetRefreshToken());
        }

        List<Result<Void, UserError>> results = runConcurrentLogouts(refreshTokens);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(countRevokedRefreshTokensForStressUser()).isEqualTo(50);
    }

    @Test
    @DisplayName("completes concurrent logout futures without execution exceptions")
    void execute_completesConcurrentLogoutsWithoutExecutionException() throws Exception {
        List<String> refreshTokens = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            refreshTokens.add(loginAndGetRefreshToken());
        }

        List<Result<Void, UserError>> results = runConcurrentLogouts(refreshTokens);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
    }

    private List<Result<Void, UserError>> runConcurrentLogouts(List<String> refreshTokens)
            throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(refreshTokens.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(refreshTokens.size());
        try {
            List<Future<Result<Void, UserError>>> futures = new ArrayList<>();
            for (String refreshToken : refreshTokens) {
                futures.add(executor.submit(logoutTask(refreshToken, ready, start)));
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

    private Callable<Result<Void, UserError>> logoutTask(
            String refreshToken, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return logoutUserUseCase.execute(new LogoutUserCommand(refreshToken));
        };
    }

    private String loginAndGetRefreshToken() {
        Result<LoginResultResponse, UserError> result =
                loginUserUseCase.execute(new LoginCommand(EMAIL, PASSWORD));

        assertThat(result.isSuccess()).isTrue();
        return ((Result.Success<LoginResultResponse, UserError>) result).value().refreshToken();
    }

    private List<String> copiesOf(String refreshToken, int count) {
        List<String> refreshTokens = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            refreshTokens.add(refreshToken);
        }
        return refreshTokens;
    }

    private int countRevokedRefreshTokensForStressUser() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens rt JOIN users u ON rt.user_id = u.id WHERE u.email = ? AND rt.revoked_at IS NOT NULL",
                Integer.class,
                EMAIL);
        return count == null ? 0 : count;
    }

    private void cleanupStressUser() {
        jdbcTemplate.update(
                "DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email = ?)",
                EMAIL);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", EMAIL);
    }
}
