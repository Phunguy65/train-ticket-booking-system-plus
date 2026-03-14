package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResult;
import io.github.phunguy65.ttbs.backend.shared.domain.SortDirection;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<UserResponse> execute(
            int page, int size, String sortField, SortDirection direction) {
        PageResult<User> users = userRepository.findAll(page, size, sortField, direction);
        return PageResult.of(
                users.items().stream().map(this::toDto).toList(),
                users.pageNumber(),
                users.pageSize(),
                users.hasNext());
    }

    private UserResponse toDto(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole().name(),
                user.getCreatedAt());
    }
}
