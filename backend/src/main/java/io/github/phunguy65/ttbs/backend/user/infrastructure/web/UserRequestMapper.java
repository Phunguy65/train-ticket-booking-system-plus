package io.github.phunguy65.ttbs.backend.user.infrastructure.web;

import io.github.phunguy65.ttbs.backend.user.application.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
class UserRequestMapper {

    UserHttpResponse toResponse(UserDto dto) {
        return new UserHttpResponse(
                dto.id(),
                dto.email(),
                dto.fullName(),
                dto.phone(),
                dto.role().name(),
                dto.createdAt());
    }
}
