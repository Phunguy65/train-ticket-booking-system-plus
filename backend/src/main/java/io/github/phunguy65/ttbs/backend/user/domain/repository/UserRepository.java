package io.github.phunguy65.ttbs.backend.user.domain.repository;

import io.github.phunguy65.ttbs.backend.shared.domain.PageResponse;
import io.github.phunguy65.ttbs.backend.shared.domain.SortOrder;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.model.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UserId id);

    PageResponse<User> findAll(int page, int size, List<SortOrder> sort);

    void softDeleteById(UserId id, Instant deletedAt);

    int softDeleteByIds(List<UserId> ids, Instant deletedAt);
}
