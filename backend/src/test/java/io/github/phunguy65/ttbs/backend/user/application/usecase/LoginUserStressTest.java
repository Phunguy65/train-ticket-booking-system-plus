package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.LoginCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.LoginResultResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import java.util.ArrayList;
import java.util.HashSet;
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
@DisplayName("LoginUserUseCase stress")
class LoginUserStressTest {

    private static final String EMAIL = "stress-login@example.com";
    private static final String PASSWORD = "secret123";

    @Autowired
    private LoginUserUseCase loginUserUseCase;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupStressUser();
        registerUserUseCase.execute(new RegisterUserCommand(EMAIL, PASSWORD, "Stress Login User"));
    }

    @AfterEach
    void tearDown() {
        cleanupStressUser();
    }

    @Test
    @DisplayName("allows 50 concurrent logins with the same valid credentials")
    void execute_allowsFiftyConcurrentLoginsWithSameValidCredentials() throws Exception {
        List<Result<LoginResultResponse, UserError>> results = runConcurrentLogins(50, PASSWORD);

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
    }

    @Test
    @DisplayName("returns InvalidCredentials for 50 concurrent wrong-password logins")
    void execute_returnsInvalidCredentialsForFiftyConcurrentWrongPasswordLogins() throws Exception {
        List<Result<LoginResultResponse, UserError>> results =
                runConcurrentLogins(50, "wrong-password");

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isFailure);
        assertThat(results.stream()
                        .map(result ->
                                ((Result.Failure<LoginResultResponse, UserError>) result).error()))
                .allMatch(UserError.InvalidCredentials.class::isInstance);
    }

    @Test
    @DisplayName("generates a distinct refresh token for each successful concurrent login")
    void execute_generatesDistinctRefreshTokensForConcurrentLogins() throws Exception {
        List<Result<LoginResultResponse, UserError>> results = runConcurrentLogins(50, PASSWORD);

        List<String> refreshTokens = results.stream()
                .map(result -> ((Result.Success<LoginResultResponse, UserError>) result).value())
                .map(LoginResultResponse::refreshToken)
                .toList();
        assertThat(new HashSet<>(refreshTokens)).hasSize(50);
        assertThat(countRefreshTokensForStressUser()).isGreaterThanOrEqualTo(50);
    }

    private List<Result<LoginResultResponse, UserError>> runConcurrentLogins(
            int threadCount, String password) throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<LoginResultResponse, UserError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(loginTask(password, ready, start)));
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();

            List<Result<LoginResultResponse, UserError>> results = new ArrayList<>();
            for (Future<Result<LoginResultResponse, UserError>> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<Result<LoginResultResponse, UserError>> loginTask(
            String password, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return loginUserUseCase.execute(new LoginCommand(EMAIL, password));
        };
    }

    private int countRefreshTokensForStressUser() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens rt JOIN users u ON rt.user_id = u.id WHERE u.email = ?",
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
