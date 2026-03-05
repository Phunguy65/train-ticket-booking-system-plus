package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.CreateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.dto.CreateUserResult;
import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public CreateUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<CreateUserResult, UserError> execute(CreateUserCommand command) {
        if (userRepository.findByEmail(command.email()).isPresent()) {
            return Result.failure(new UserError.EmailAlreadyExists());
        }

        String temporaryPassword = UUID.randomUUID().toString().replace("-", "");
        String passwordHash = passwordEncoder.encode(temporaryPassword);
        UserId userId = UserId.of(UUID.randomUUID());
        User user = User.create(
                userId, command.email(), passwordHash, command.fullName(), command.phone());
        User saved = userRepository.save(user);

        for (DomainEvent event : user.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        user.clearDomainEvents();

        return Result.success(new CreateUserResult(toDto(saved), temporaryPassword));
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId().value(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt());
    }
}
