package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import com.sashaprylutsky.wishplus.repository.UserRepository;
import com.sashaprylutsky.wishplus.util.JwtUtil;
import jakarta.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

@Service
public class UserService implements UserDetailsService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(BCryptPasswordEncoder encoder,
                       UserRepository userRepository,
                       JwtUtil jwtUtil) {
        this.encoder = encoder;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public static UserPrincipal getUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof UserPrincipal)) {
            String principalType = (authentication != null && authentication.getPrincipal() != null)
                    ? authentication.getPrincipal().getClass().getName() : "null";
            log.debug("Authentication check failed or principal type mismatch. Is Authenticated: {}, Principal Type: {}",
                    (authentication != null && authentication.isAuthenticated()), principalType);
            throw new NullPointerException("User is not authenticated.");
        }

        return (UserPrincipal) authentication.getPrincipal();
    }

    public User registerUser(User user) {
        try {
            User createdUser = new User(user.getEmail(),
                    user.getUsername(),
                    encoder.encode(user.getPassword()),
                    user.getFirstName(),
                    user.getLastName());
            return userRepository.save(createdUser);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateKeyException("Email is already taken.");
        }
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoResultException("No user found with ID " + id));
    }

    public User updateUserPrincipalDetails(Long id, User user) {
        UserPrincipal userPrincipal = getUserPrincipal();
        User userRecord = getUserById(id);

        if (!Objects.equals(userPrincipal.getId(), userRecord.getId())) {
            throw new AccessDeniedException("Change access is prohibited.");
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            userRecord.setUsername(user.getUsername());
        }
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            userRecord.setFirstName(user.getFirstName());
        }

        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            userRecord.setLastName(user.getLastName());
        }

        if (user.getProfilePhoto() != null && !user.getProfilePhoto().isBlank()) {
            userRecord.setProfilePhoto(user.getProfilePhoto());
        }

        return userRepository.save(userRecord);
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public void deleteUserById(Long id, String submitMessage) {
        UserPrincipal userPrincipal = getUserPrincipal();
        User userRecord = userRepository.findById(id).orElseThrow(() ->
                new NoResultException("No user found with ID " + id));

        final String submitPhrase = "Delete my account forever!";

        if (!Objects.equals(userPrincipal.getId(), userRecord.getId())) {
            throw new AccessDeniedException("Access to delete is denied.");
        }
        if (!submitMessage.equals(submitPhrase)) {
            throw new CancellationException("The deletion is aborted.");
        }

        userRepository.delete(userRecord);
    }

    public String login(User user) {
        User userRecord = getUserByUsername(user.getUsername());

        if (encoder.matches(user.getPassword(), userRecord.getPassword())) {
            return jwtUtil.generateToken(userRecord);
        }
        throw new RuntimeException("Invalid credentials");
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NoResultException("No user found with username: " + username));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = getUserByUsername(username);

        return new UserPrincipal(user.getId(), user.getEmail(), user.getUsername(), user.getPassword());
    }
}
