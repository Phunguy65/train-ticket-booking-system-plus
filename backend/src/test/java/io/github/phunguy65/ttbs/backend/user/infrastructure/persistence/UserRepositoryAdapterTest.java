package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import({UserRepositoryAdapter.class, UserEntityMapper.class})
@TestPropertySource(properties = "spring.modulith.detection.disabled=true")
class UserRepositoryAdapterTest {

    @Autowired
    private UserRepository userRepository;

    // ── Existing tests ───────────────────────────────────────────────────────

    @Test
    void save_shouldPersistUser() {
        User user = User.create(
                UserId.of(UUID.randomUUID()),
                "test@example.com",
                "$2a$12$hashedPassword",
                "Test User",
                "0901234567");

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByEmail_shouldReturnUserWhenExists() {
        User user = User.create(
                UserId.of(UUID.randomUUID()), "find@example.com", "$2a$12$hash", "Find User", null);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("find@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find@example.com");
        assertThat(found.get().getDomainEvents()).isEmpty();
    }

    @Test
    void findByEmail_unknownEmail_shouldReturnEmpty() {
        Optional<User> found = userRepository.findByEmail("nobody@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void findById_shouldReturnUserWhenExists() {
        UserId id = UserId.of(UUID.randomUUID());
        User user = User.create(id, "byid@example.com", "$2a$12$hash", "By Id", null);
        userRepository.save(user);

        Optional<User> found = userRepository.findById(id);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(id);
    }

    // ── findAll slice tests ──────────────────────────────────────────────────

    @Test
    void findAll_emptyDatabase_returnsEmptyPageResult() {
        PageResult<User> result = userRepository.findAll(0, 20, "createdAt", SortDirection.DESC);

        assertThat(result.items()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_firstPage_returnsItemsWithCorrectMetadata() {
        for (int i = 0; i < 5; i++) {
            userRepository.save(User.create(
                    UserId.of(UUID.randomUUID()),
                    "user" + i + "@example.com",
                    "$2a$12$hash",
                    "User " + i,
                    null));
        }

        PageResult<User> result = userRepository.findAll(0, 3, "email", SortDirection.ASC);

        assertThat(result.items()).hasSize(3);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void findAll_lastPage_hasNextFalseHasPreviousTrue() {
        for (int i = 0; i < 4; i++) {
            userRepository.save(User.create(
                    UserId.of(UUID.randomUUID()),
                    "page" + i + "@example.com",
                    "$2a$12$hash",
                    "Page User " + i,
                    null));
        }

        // page=1, size=3: should return 1 item (the 4th), hasNext=false, hasPrevious=true
        PageResult<User> result = userRepository.findAll(1, 3, "email", SortDirection.ASC);

        assertThat(result.items()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void findAll_sortByEmailAsc_returnsItemsInOrder() {
        userRepository.save(User.create(
                UserId.of(UUID.randomUUID()), "zebra@example.com", "$2a$12$hash", "Zebra", null));
        userRepository.save(User.create(
                UserId.of(UUID.randomUUID()), "apple@example.com", "$2a$12$hash", "Apple", null));
        userRepository.save(User.create(
                UserId.of(UUID.randomUUID()), "mango@example.com", "$2a$12$hash", "Mango", null));

        PageResult<User> result = userRepository.findAll(0, 10, "email", SortDirection.ASC);

        assertThat(result.items())
                .extracting(User::getEmail)
                .containsExactly("apple@example.com", "mango@example.com", "zebra@example.com");
    }

    @Test
    void findAll_exactlyOnePage_hasNextFalse() {
        for (int i = 0; i < 3; i++) {
            userRepository.save(User.create(
                    UserId.of(UUID.randomUUID()),
                    "exact" + i + "@example.com",
                    "$2a$12$hash",
                    "Exact " + i,
                    null));
        }

        PageResult<User> result = userRepository.findAll(0, 3, "email", SortDirection.ASC);

        assertThat(result.items()).hasSize(3);
        assertThat(result.hasNext()).isFalse();
    }
}
