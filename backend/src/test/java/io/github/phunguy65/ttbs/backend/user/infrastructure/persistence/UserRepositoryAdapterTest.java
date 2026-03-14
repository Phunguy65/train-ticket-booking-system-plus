package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
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
        PageResponse<User> result =
                userRepository.findAll(0, 20, List.of(SortOrder.desc("createdAt")));

        assertThat(result.content()).isEmpty();
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

        PageResponse<User> result = userRepository.findAll(0, 3, List.of(SortOrder.asc("email")));

        assertThat(result.content()).hasSize(3);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(3);
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
        PageResponse<User> result = userRepository.findAll(1, 3, List.of(SortOrder.asc("email")));

        assertThat(result.content()).hasSize(1);
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

        PageResponse<User> result = userRepository.findAll(0, 10, List.of(SortOrder.asc("email")));

        assertThat(result.content())
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

        PageResponse<User> result = userRepository.findAll(0, 3, List.of(SortOrder.asc("email")));

        assertThat(result.content()).hasSize(3);
        assertThat(result.hasNext()).isFalse();
    }

    // ── Soft-delete filter tests ──────────────────────────────────────────────

    @Test
    void findByEmail_softDeletedUser_shouldReturnEmpty() {
        UserId id = UserId.of(UUID.randomUUID());
        User user = User.create(id, "deleted@example.com", "$2a$12$hash", "Deleted User", null);
        userRepository.save(user);
        userRepository.softDeleteById(id, Instant.now());

        Optional<User> found = userRepository.findByEmail("deleted@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void findById_softDeletedUser_shouldReturnEmpty() {
        UserId id = UserId.of(UUID.randomUUID());
        User user = User.create(id, "deleted2@example.com", "$2a$12$hash", "Deleted2", null);
        userRepository.save(user);
        userRepository.softDeleteById(id, Instant.now());

        Optional<User> found = userRepository.findById(id);

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_softDeletedUser_shouldNotBeIncluded() {
        UserId activeId = UserId.of(UUID.randomUUID());
        UserId deletedId = UserId.of(UUID.randomUUID());
        userRepository.save(
                User.create(activeId, "active@example.com", "$2a$12$hash", "Active", null));
        userRepository.save(
                User.create(deletedId, "gone@example.com", "$2a$12$hash", "Gone", null));
        userRepository.softDeleteById(deletedId, Instant.now());

        PageResponse<User> result = userRepository.findAll(0, 20, List.of(SortOrder.asc("email")));

        assertThat(result.content())
                .extracting(User::getEmail)
                .containsExactly("active@example.com");
    }

    @Test
    void softDeleteById_shouldSetDeletedAt() {
        UserId id = UserId.of(UUID.randomUUID());
        userRepository.save(User.create(id, "todel@example.com", "$2a$12$hash", "ToDelete", null));
        Instant before = Instant.now();

        userRepository.softDeleteById(id, before);

        assertThat(userRepository.findById(id)).isEmpty();
    }

    @Test
    void softDeleteByIds_bulkDelete_shouldReturnAffectedCount() {
        UserId id1 = UserId.of(UUID.randomUUID());
        UserId id2 = UserId.of(UUID.randomUUID());
        UserId id3 = UserId.of(UUID.randomUUID());
        userRepository.save(User.create(id1, "bulk1@example.com", "$2a$12$hash", "Bulk1", null));
        userRepository.save(User.create(id2, "bulk2@example.com", "$2a$12$hash", "Bulk2", null));
        userRepository.save(User.create(id3, "bulk3@example.com", "$2a$12$hash", "Bulk3", null));

        int affected = userRepository.softDeleteByIds(List.of(id1, id2), Instant.now());

        assertThat(affected).isEqualTo(2);
        assertThat(userRepository.findById(id1)).isEmpty();
        assertThat(userRepository.findById(id2)).isEmpty();
        assertThat(userRepository.findById(id3)).isPresent();
    }
}
