package io.github.phunguy65.ttbs.backend.user;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.BookingValidationPort;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.application.usecase.ListUsersUseCase;
import io.github.phunguy65.ttbs.backend.user.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserRegistered;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@TestPropertySource(
        properties = {
            "jwt.secret=test-secret-key-that-is-long-enough-for-hs256-algorithm",
            "jwt.access-token-expiry=900",
            "jwt.refresh-token-expiry=604800"
        })
class UserModuleTest {

    @MockitoBean
    private BookingValidationPort bookingValidationPort;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;

    @Autowired
    private ListUsersUseCase listUsersUseCase;

    @Test
    void userModule_isStructurallyValid() {
        // Spring Modulith verifies module structure upon context loading.
        // If this test starts successfully, module boundaries are valid.
    }

    @Test
    void registerUser_publishesUserRegisteredEvent(PublishedEvents events) {
        RegisterUserCommand command = new RegisterUserCommand(
                "moduletest@example.com", "password123", "Module Test User", null);

        registerUserUseCase.execute(command);

        var userRegisteredEvents = events.ofType(UserRegistered.class);
        assertThat(userRegisteredEvents)
                .as("Expected UserRegistered event to be published")
                .hasSize(1);
        assertThat(userRegisteredEvents.iterator().next().email())
                .isEqualTo("moduletest@example.com");
    }

    @Test
    void listUsers_emptyDatabase_returnsEmptySlice() {
        PageResult<UserResponse> result =
                listUsersUseCase.execute(0, 20, "createdAt", SortDirection.DESC);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(20);
    }

    @Test
    void listUsers_afterRegisteringUsers_returnsUsersInSlice(PublishedEvents events) {
        registerUserUseCase.execute(
                new RegisterUserCommand("list1@example.com", "password123", "List User One", null));
        registerUserUseCase.execute(
                new RegisterUserCommand("list2@example.com", "password456", "List User Two", null));

        PageResult<UserResponse> result =
                listUsersUseCase.execute(0, 10, "email", SortDirection.ASC);

        assertThat(result.items())
                .hasSizeGreaterThanOrEqualTo(2)
                .extracting(UserResponse::email)
                .contains("list1@example.com", "list2@example.com");
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void listUsers_pageSizeOne_hasNextWhenMultipleUsersExist(PublishedEvents events) {
        registerUserUseCase.execute(
                new RegisterUserCommand("page1@example.com", "password123", "Page User One", null));
        registerUserUseCase.execute(
                new RegisterUserCommand("page2@example.com", "password456", "Page User Two", null));

        PageResult<UserResponse> result =
                listUsersUseCase.execute(0, 1, "email", SortDirection.ASC);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }
}
