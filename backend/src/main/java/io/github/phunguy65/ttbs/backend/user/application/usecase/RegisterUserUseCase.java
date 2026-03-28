package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.user.application.command.RegisterUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponseMapper;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserResponseMapper userResponseMapper;

    public RegisterUserUseCase(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional
    public Result<UserResponse, UserError> execute(RegisterUserCommand command) {
        EmailAddress email = EmailAddress.of(command.email());
        if (userRepository.findByEmail(email.value()).isPresent()) {
            return Result.failure(new UserError.EmailAlreadyExists());
        }

        PasswordHash passwordHash = PasswordHash.of(passwordEncoder.encode(command.password()));
        UserId userId = UserId.of(UuidGenerator.generate());
        User user = User.create(
                userId,
                email,
                passwordHash,
                PersonName.of(command.fullName()),
                null,
                null,
                null,
                null,
                null);
        User saved = userRepository.save(user);

        for (DomainEvent event : user.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        user.clearDomainEvents();

        return Result.success(userResponseMapper.fromUser(saved));
    }
}
