package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.application.response.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.application.query.GetUsersQuery;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> execute(GetUsersQuery query) {
        List<SortOrder> sort = List.of(SortOrder.desc("createdAt"), SortOrder.asc("id"));
        PageResponse<User> users = userRepository.findAll(query.page(), query.size(), sort);
        return PageResponse.of(
                users.content().stream().map(this::toDto).toList(),
                users.page(),
                users.size(),
                users.hasNext(),
                users.total());
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
