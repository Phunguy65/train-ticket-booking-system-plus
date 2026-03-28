package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.AddressLine;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.EmailAddress;
import io.github.phunguy65.ttbs.backend.shared.domain.Gender;
import io.github.phunguy65.ttbs.backend.shared.domain.IdDocumentNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.PasswordHash;
import io.github.phunguy65.ttbs.backend.shared.domain.PersonName;
import io.github.phunguy65.ttbs.backend.shared.domain.PhoneNumber;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.shared.domain.UuidGenerator;
import io.github.phunguy65.ttbs.backend.user.application.command.CreateUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.PasswordEncoder;
import io.github.phunguy65.ttbs.backend.user.application.response.CreateUserResponse;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponseMapper;
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
    private final UserResponseMapper userResponseMapper;

    public CreateUserUseCase(
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
    public Result<CreateUserResponse, UserError> execute(CreateUserCommand command) {
        EmailAddress email = EmailAddress.of(command.email());
        if (userRepository.findByEmail(email.value()).isPresent()) {
            return Result.failure(new UserError.EmailAlreadyExists());
        }

        String temporaryPassword = UUID.randomUUID().toString().replace("-", "");
        PasswordHash passwordHash = PasswordHash.of(passwordEncoder.encode(temporaryPassword));
        UserId userId = UserId.of(UuidGenerator.generate());
        User user = User.create(
                userId,
                email,
                passwordHash,
                PersonName.of(command.fullName()),
                PhoneNumber.ofNullable(command.phone()),
                command.dateOfBirth(),
                Gender.ofNullable(command.gender()),
                IdDocumentNumber.ofNullable(command.idDocumentNumber()),
                AddressLine.ofNullable(command.addressLine()));
        User saved = userRepository.save(user);

        for (DomainEvent event : user.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        user.clearDomainEvents();

        return Result.success(
                new CreateUserResponse(userResponseMapper.fromUser(saved), temporaryPassword));
    }
}
