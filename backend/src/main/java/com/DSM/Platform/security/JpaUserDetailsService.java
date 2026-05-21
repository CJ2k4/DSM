package com.DSM.Platform.security;

import com.DSM.Platform.user.UserRepository;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public JpaUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(identifier, identifier)
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public AuthenticatedUser loadUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
