package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.booking.domain.repository.BookingRepository;
import io.github.phunguy65.ttbs.backend.shared.domain.DomainEvent;
import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.SoftDeleteUserCommand;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteAuthenticatedUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeleteAuthenticatedUserUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            BookingRepository bookingRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.bookingRepository = bookingRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, UserError> execute(SoftDeleteUserCommand command) {
        Optional<User> found = userRepository.findByIdIncludingDeleted(command.userId());
        if (found.isEmpty()) {
            return Result.failure(new UserError.UserNotFound());
        }

        User user = found.get();

        // Idempotent: already deleted → return success immediately
        if (user.isDeleted()) {
            return Result.success();
        }

        if (bookingRepository.existsActiveByUserId(command.userId())) {
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
