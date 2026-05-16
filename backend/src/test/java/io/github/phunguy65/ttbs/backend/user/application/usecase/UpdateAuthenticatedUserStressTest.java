package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.ttbs.backend.TestContainerConfiguration;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.command.UpdateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
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
@DisplayName("UpdateAuthenticatedUserUseCase stress")
class UpdateAuthenticatedUserStressTest {

    private static final String EMAIL = "stress-update@example.com";
    private static final String EMAIL_PREFIX = "stress-";
    private static final String PASSWORD = "secret123";

    @Autowired
    private UpdateAuthenticatedUserUseCase updateAuthenticatedUserUseCase;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupStressUsers();
        registerUserUseCase.execute(new RegisterUserCommand(EMAIL, PASSWORD, "Stress Update User"));
    }

    @AfterEach
    void tearDown() {
        cleanupStressUsers();
    }

    @Test
    @DisplayName("allows 50 concurrent profile updates on the same user")
    void execute_allowsFiftyConcurrentProfileUpdatesOnSameUser() throws Exception {
        UUID userId = findUserIdByEmail(EMAIL);

        List<Result<UserResponse, UserError>> results = runConcurrentUpdates(
                50,
                index -> new UpdateUserCommand(
                        UserId.of(userId),
                        "User-" + index,
                        EMAIL,
                        "+84901234567",
                        LocalDate.of(1995, 5, 15),
                        "female",
                        "012345678901",
                        "123 Stress Street"));

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(expectedNames()).contains(findFullNameByEmail(EMAIL));
    }

    @Test
    @DisplayName("handles 50 concurrent email changes to different emails")
    void execute_handlesFiftyConcurrentEmailChangesToDifferentEmails() throws Exception {
        UUID userId = findUserIdByEmail(EMAIL);

        List<Result<UserResponse, UserError>> results = runConcurrentUpdates(
                50,
                index -> new UpdateUserCommand(
                        UserId.of(userId),
                        "Stress Email User " + index,
                        EMAIL_PREFIX + index + "@example.com",
                        "+84901234567",
                        LocalDate.of(1995, 5, 15),
                        "female",
                        "012345678901",
                        "123 Stress Street"));

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(Result::isSuccess);
        assertThat(expectedEmails()).contains(findEmailById(userId));
    }

    private List<Result<UserResponse, UserError>> runConcurrentUpdates(
            int threadCount, CommandFactory commandFactory)
            throws InterruptedException, ExecutionException {
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Result<UserResponse, UserError>>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(updateTask(commandFactory.create(i), ready, start)));
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

    private Callable<Result<UserResponse, UserError>> updateTask(
            UpdateUserCommand command, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            return updateAuthenticatedUserUseCase.execute(command);
        };
    }

    private UUID findUserIdByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    private String findFullNameByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT full_name FROM users WHERE email = ?", String.class, email);
    }

    private String findEmailById(UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, userId);
    }

    private HashSet<String> expectedNames() {
        HashSet<String> names = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            names.add("User-" + i);
        }
        return names;
    }

    private HashSet<String> expectedEmails() {
        HashSet<String> emails = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            emails.add(EMAIL_PREFIX + i + "@example.com");
        }
        return emails;
    }

    private void cleanupStressUsers() {
        jdbcTemplate.update(
                "DELETE FROM users WHERE email = ? OR email LIKE ?",
                EMAIL,
                EMAIL_PREFIX + "%@example.com");
    }

    @FunctionalInterface
    private interface CommandFactory {
        UpdateUserCommand create(int index);
    }
}
