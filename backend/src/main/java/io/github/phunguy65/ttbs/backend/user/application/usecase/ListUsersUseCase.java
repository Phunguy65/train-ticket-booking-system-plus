package io.github.phunguy65.ttbs.backend.user.application.usecase;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.application.query.GetUsersQuery;
import io.github.phunguy65.ttbs.backend.user.application.response.UserResponse;
import io.github.phunguy65.ttbs.backend.user.domain.projection.UserSummary;
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
        PageResponse<UserSummary> summaries =
                userRepository.findAllSummaries(query.page(), query.size(), sort);
        return PageResponse.of(
                summaries.content().stream()
                        .map(s -> new UserResponse(
                                s.id(),
                                s.email(),
                                s.fullName(),
                                s.phone(),
                                s.role(),
                                s.createdAt()))
                        .toList(),
                summaries.page(),
                summaries.size(),
                summaries.hasNext(),
                summaries.total());
    }
}
