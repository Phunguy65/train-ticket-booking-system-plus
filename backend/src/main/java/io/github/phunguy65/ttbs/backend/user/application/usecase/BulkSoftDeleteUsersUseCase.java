package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.Result;
import io.github.phunguy65.ttbs.backend.user.application.command.BulkSoftDeleteUsersCommand;
import io.github.phunguy65.ttbs.backend.user.application.port.BookingValidationPort;
import io.github.phunguy65.ttbs.backend.user.domain.error.UserError;
import io.github.phunguy65.ttbs.backend.user.domain.event.UserDeleted;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.repository.RefreshTokenRepository;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkSoftDeleteUsersUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BookingValidationPort bookingValidationPort;
    private final ApplicationEventPublisher eventPublisher;

    public BulkSoftDeleteUsersUseCase(
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
    public Result<Integer, UserError> execute(BulkSoftDeleteUsersCommand command) {
        // Pre-validate: check ALL user IDs for active bookings
        List<UserId> conflictingIds = command.userIds().stream()
                .filter(bookingValidationPort::hasActiveBookingsForUser)
                .toList();

        if (!conflictingIds.isEmpty()) {
            return Result.failure(new UserError.UserHasActiveBookings());
        }

        Instant now = Instant.now();
        int affected = userRepository.softDeleteByIds(command.userIds(), now);
        refreshTokenRepository.revokeAllByUserIds(command.userIds());

        for (UserId userId : command.userIds()) {
            eventPublisher.publishEvent(UserDeleted.of(userId));
        }

        return Result.success(affected);
    }
}
