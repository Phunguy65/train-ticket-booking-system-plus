package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.SoftDeleteUserCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.BookingValidationPort;
import io.github.phunguy65.ttbs.backend.user.domain.errors.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftDeleteUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BookingValidationPort bookingValidationPort;
    private final ApplicationEventPublisher eventPublisher;

    public SoftDeleteUserUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            BookingValidationPort bookingValidationPort,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.bookingValidationPort = bookingValidationPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, UserError> execute(SoftDeleteUserCommand command) {
        Optional<User> found = userRepository.findById(command.userId());
        if (found.isEmpty()) {
            return Result.failure(new UserError.UserNotFound());
        }

        User user = found.get();

        // Idempotent: already deleted → return success immediately
        if (user.isDeleted()) {
            return Result.success();
        }

        if (bookingValidationPort.hasActiveBookingsForUser(command.userId())) {
            return Result.failure(new UserError.UserHasActiveBookings());
        }

        Result<Void, UserError> deleteResult = user.softDelete();
        if (deleteResult.isFailure()) {
            return deleteResult;
        }

        refreshTokenRepository.revokeAllByUserId(command.userId());
        userRepository.save(user);

        for (DomainEvent event : user.getDomainEvents()) {
            eventPublisher.publishEvent(event);
        }
        user.clearDomainEvents();

        return Result.success();
    }
}
