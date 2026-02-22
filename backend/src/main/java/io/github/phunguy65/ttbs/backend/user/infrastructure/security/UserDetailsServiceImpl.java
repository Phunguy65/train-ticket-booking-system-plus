package io.github.phunguy65.ttbs.backend.user.infrastructure.security;

import io.github.phunguy65.ttbs.backend.shared.domain.UserId;
import io.github.phunguy65.ttbs.backend.user.domain.model.User;
import io.github.phunguy65.ttbs.backend.user.domain.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads user by ID (UUID string). Called by {@link JwtAuthenticationFilter} after token validation.
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserId id = UserId.of(UUID.fromString(userId));
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        return new org.springframework.security.core.userdetails.User(
                user.getId().value().toString(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
