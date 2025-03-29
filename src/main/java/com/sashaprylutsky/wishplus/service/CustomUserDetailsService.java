package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import com.sashaprylutsky.wishplus.repository.UserRepository;
import jakarta.persistence.NoResultException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoResultException("No user found with username: " + username));

        if (user == null) {
            throw new UsernameNotFoundException(String.format("User \"%s\" not found.", username));
        } else {
            return new UserPrincipal(user.getId(), user.getEmail(), user.getUsername(), user.getPassword());
        }
    }
}
