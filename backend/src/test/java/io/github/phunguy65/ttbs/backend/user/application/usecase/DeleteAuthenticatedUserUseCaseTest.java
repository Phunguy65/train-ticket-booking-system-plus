package io.github.phunguy65.ttbs.backend.user.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.SoftDeleteUserCommand;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserDeleted;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserRole;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteAuthenticatedUserUseCase")
class DeleteAuthenticatedUserUseCaseTest {

    private static final UUID USER_UUID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UserId USER_ID = UserId.of(USER_UUID);
    private static final Instant CREATED_AT = Instant.parse("2026-05-15T10:15:30Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-05-15T10:15:30Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DeleteAuthenticatedUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteAuthenticatedUserUseCase(
                userRepository, refreshTokenRepository, bookingRepository, eventPublisher);
    }

    @Nested
    @DisplayName("happy path")
    class HappyPath {

        @Test
        @DisplayName("soft-deletes user, revokes tokens, publishes event and returns success")
        void execute_softDeletesUserAndReturnsSuccess() {
            User activeUser = activeUser();
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
            when(userRepository.save(activeUser)).thenReturn(activeUser);

            Result<Void, UserError> result = useCase.execute(new SoftDeleteUserCommand(USER_ID));

            assertThat(result.isSuccess()).isTrue();
            verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            verify(userRepository).save(activeUser);
            ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isInstanceOf(UserDeleted.class);
        }
    }

    @Nested
    @DisplayName("idempotent behavior")
    class IdempotentBehavior {

        @Test
        @DisplayName("returns success immediately when user is already deleted")
        void execute_returnsSuccessWhenUserAlreadyDeleted() {
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(deletedUser()));

            Result<Void, UserError> result = useCase.execute(new SoftDeleteUserCommand(USER_ID));

            assertThat(result.isSuccess()).isTrue();
            verify(bookingRepository, never()).existsActiveByUserId(USER_ID);
            verifyNoInteractions(refreshTokenRepository, eventPublisher);
            verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        }
    }

    @Nested
    @DisplayName("failure path")
    class FailurePath {

        @Test
        @DisplayName("returns UserNotFound when repository returns empty")
        void execute_returnsUserNotFoundWhenRepositoryReturnsEmpty() {
            when(userRepository.findByIdIncludingDeleted(USER_ID)).thenReturn(Optional.empty());

            Result<Void, UserError> result = useCase.execute(new SoftDeleteUserCommand(USER_ID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<Void, UserError>) result).error())
                    .isInstanceOf(UserError.UserNotFound.class);
        }

        @Test
        @DisplayName("returns UserHasActiveBookings when active bookings exist")
        void execute_returnsUserHasActiveBookingsWhenActiveBookingsExist() {
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser()));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(true);

            Result<Void, UserError> result = useCase.execute(new SoftDeleteUserCommand(USER_ID));

            assertThat(result.isFailure()).isTrue();
            assertThat(((Result.Failure<Void, UserError>) result).error())
                    .isInstanceOf(UserError.UserHasActiveBookings.class);
            verify(refreshTokenRepository, never()).revokeAllByUserId(USER_ID);
            verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    @DisplayName("internal behavior")
    class InternalBehavior {

        @Test
        @DisplayName("calls findById with exact userId from command")
        void execute_callsFindByIdWithExactUserIdFromCommand() {
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(deletedUser()));

            useCase.execute(new SoftDeleteUserCommand(USER_ID));

            verify(userRepository).findByIdIncludingDeleted(USER_ID);
        }

        @Test
        @DisplayName("calls existsActiveByUserId with exact userId")
        void execute_callsExistsActiveByUserIdWithExactUserId() {
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser()));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(true);

            useCase.execute(new SoftDeleteUserCommand(USER_ID));

            verify(bookingRepository).existsActiveByUserId(USER_ID);
        }

        @Test
        @DisplayName("calls revokeAllByUserId before save")
        void execute_callsRevokeAllBeforeSave() {
            User activeUser = activeUser();
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
            when(userRepository.save(activeUser)).thenReturn(activeUser);

            useCase.execute(new SoftDeleteUserCommand(USER_ID));

            InOrder inOrder = inOrder(refreshTokenRepository, userRepository);
            inOrder.verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
            inOrder.verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("publishes domain events after save")
        void execute_publishesDomainEventsAfterSave() {
            User activeUser = activeUser();
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
            when(userRepository.save(activeUser)).thenReturn(activeUser);

            useCase.execute(new SoftDeleteUserCommand(USER_ID));

            InOrder inOrder = inOrder(userRepository, eventPublisher);
            inOrder.verify(userRepository).save(activeUser);
            inOrder.verify(eventPublisher)
                    .publishEvent(org.mockito.ArgumentMatchers.any(UserDeleted.class));
        }

        @Test
        @DisplayName("clears domain events after publishing")
        void execute_clearsDomainEventsAfterPublishing() {
            User activeUser = activeUser();
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
            when(userRepository.save(activeUser)).thenReturn(activeUser);

            useCase.execute(new SoftDeleteUserCommand(USER_ID));

            assertThat(activeUser.getDomainEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("exception handling")
    class ExceptionHandling {

        @Test
        @DisplayName("propagates findById failures")
        void execute_propagatesFindByIdFailures() {
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenThrow(new RuntimeException("find failed"));

            assertThatThrownBy(() -> useCase.execute(new SoftDeleteUserCommand(USER_ID)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("find failed");
        }

        @Test
        @DisplayName("propagates existsActiveByUserId failures")
        void execute_propagatesExistsActiveByUserIdFailures() {
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser()));
            when(bookingRepository.existsActiveByUserId(USER_ID))
                    .thenThrow(new RuntimeException("exists failed"));

            assertThatThrownBy(() -> useCase.execute(new SoftDeleteUserCommand(USER_ID)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("exists failed");
        }

        @Test
        @DisplayName("propagates revokeAllByUserId failures")
        void execute_propagatesRevokeAllByUserIdFailures() {
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser()));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
            org.mockito.Mockito.doThrow(new RuntimeException("revoke failed"))
                    .when(refreshTokenRepository)
                    .revokeAllByUserId(USER_ID);

            assertThatThrownBy(() -> useCase.execute(new SoftDeleteUserCommand(USER_ID)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("revoke failed");
        }

        @Test
        @DisplayName("propagates save failures")
        void execute_propagatesSaveFailures() {
            User activeUser = activeUser();
            when(userRepository.findByIdIncludingDeleted(USER_ID))
                    .thenReturn(Optional.of(activeUser));
            when(bookingRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
            when(userRepository.save(activeUser)).thenThrow(new RuntimeException("save failed"));

            assertThatThrownBy(() -> useCase.execute(new SoftDeleteUserCommand(USER_ID)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("save failed");
        }
    }

    private User activeUser() {
        return User.reconstitute(
                USER_ID,
                EmailAddress.of("delete-me@example.com"),
                PasswordHash.of("hashed-secret"),
                PersonName.of("Delete Me"),
                PhoneNumber.ofNullable("+84901234567"),
                LocalDate.of(1995, 5, 15),
                Gender.ofNullable("female"),
                IdDocumentNumber.ofNullable("012345678901"),
                AddressLine.ofNullable("123 Test Street"),
                UserRole.CUSTOMER,
                CREATED_AT,
                UPDATED_AT,
                null);
    }

    private User deletedUser() {
        return User.reconstitute(
                USER_ID,
                EmailAddress.of("deleted@example.com"),
                PasswordHash.of("hashed-secret"),
                PersonName.of("Deleted User"),
                PhoneNumber.ofNullable("+84901234567"),
                LocalDate.of(1995, 5, 15),
                Gender.ofNullable("female"),
                IdDocumentNumber.ofNullable("012345678901"),
                AddressLine.ofNullable("123 Test Street"),
                UserRole.CUSTOMER,
                CREATED_AT,
                UPDATED_AT,
                Instant.now());
    }
}
