package io.github.phunguy65.ttbs.backend.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.*;

import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
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
}
