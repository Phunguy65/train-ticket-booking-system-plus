package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.query.GetUserByIdQuery;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAuthenticatedUserUseCase")
class GetAuthenticatedUserUseCaseTest {

    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    private UserRepository userRepository;

    private GetAuthenticatedUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAuthenticatedUserUseCase(userRepository);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("returns UserResponse when user summary is found")
        void execute_returnsUserResponseWhenUserSummaryIsFound() {
            when(userRepository.findSummaryById(UserId.of(USER_ID)))
                    .thenReturn(Optional.of(userSummary()));

            Result<UserResponse, UserError> result = useCase.execute(new GetUserByIdQuery(USER_ID));

            assertThat(result.isSuccess()).isTrue();
            UserResponse response = ((Result.Success<UserResponse, UserError>) result).value();
            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo("customer@example.com");
            assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("calls findSummaryById with the exact UserId from query")
        void execute_callsFindSummaryByIdWithExactUserIdFromQuery() {
            when(userRepository.findSummaryById(UserId.of(USER_ID)))
                    .thenReturn(Optional.of(userSummary()));

            useCase.execute(new GetUserByIdQuery(USER_ID));

            verify(userRepository).findSummaryById(UserId.of(USER_ID));
        }

        @Test
        @DisplayName("maps all UserSummary fields to UserResponse correctly")
        void execute_mapsAllUserSummaryFieldsToUserResponseCorrectly() {
            when(userRepository.findSummaryById(UserId.of(USER_ID)))
                    .thenReturn(Optional.of(userSummary()));

            Result<UserResponse, UserError> result = useCase.execute(new GetUserByIdQuery(USER_ID));

            UserResponse response = ((Result.Success<UserResponse, UserError>) result).value();
            assertThat(response.id()).isEqualTo(USER_ID);
            assertThat(response.email()).isEqualTo("customer@example.com");
            assertThat(response.fullName()).isEqualTo("Nguyen Van A");
            assertThat(response.phone()).isEqualTo("+84901234567");
            assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));
            assertThat(response.gender()).isEqualTo("female");
            assertThat(response.idDocumentNumber()).isEqualTo("012345678901");
            assertThat(response.addressLine()).isEqualTo("123 Test Street");
            assertThat(response.role()).isEqualTo("CUSTOMER");
            assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-05-15T10:15:30Z"));
        }
    }

    @Nested
    @DisplayName("failure path")
    class FailurePath {

        @Test
        @DisplayName("returns UserNotFound when repository returns empty")
        void execute_returnsUserNotFoundWhenRepositoryReturnsEmpty() {
            when(userRepository.findSummaryById(UserId.of(USER_ID))).thenReturn(Optional.empty());

            Result<UserResponse, UserError> result = useCase.execute(new GetUserByIdQuery(USER_ID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<UserResponse, UserError>) result).error())
                    .isInstanceOf(UserError.UserNotFound.class);
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates repository lookup failures")
        void execute_propagatesRepositoryLookupFailures() {
            when(userRepository.findSummaryById(UserId.of(USER_ID)))
                    .thenThrow(new RuntimeException("db down"));

            assertThatThrownBy(() -> useCase.execute(new GetUserByIdQuery(USER_ID)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("db down");
        }
    }

    private UserSummary userSummary() {
        return new UserSummary(
                USER_ID,
                "customer@example.com",
                "Nguyen Van A",
                "+84901234567",
                LocalDate.of(1995, 5, 15),
                "female",
                "012345678901",
                "123 Test Street",
                "CUSTOMER",
                Instant.parse("2026-05-15T10:15:30Z"));
    }
}
