package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.Followers;
import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.repository.UserRepository;
import com.sashaprylutsky.wishplus.security.JwtService;
import jakarta.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CancellationException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final JwtService jwtService;

    public UserService(PasswordEncoder encoder, UserRepository userRepository, JwtService jwtService) {
        this.encoder = encoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public User getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new NullPointerException("User is not authenticated.");
        }

        return (User) authentication.getPrincipal();
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
            throw new DuplicateKeyException("Email/username is already taken.");
        }
    }

    public String login(User userDTO) {
        User user = getUserByUsername(userDTO.getUsername());
        if (encoder.matches(userDTO.getPassword(), user.getPassword())) {
            return jwtService.generateToken(user);
        }
        throw new RuntimeException("Invalid credentials");
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoResultException("No user found with ID " + id));
    }

    public User updateUser(User userDTO) {
        try {
            User principal = getPrincipal();
            User user = getUserById(principal.getId());

            if (userDTO.getEmail() != null && !userDTO.getEmail().isBlank()) {
                user.setEmail(userDTO.getEmail());
            }
            if (userDTO.getUsername() != null && !userDTO.getUsername().isBlank()) {
                user.setUsername(userDTO.getUsername());
            }
            if (userDTO.getFirstName() != null && !userDTO.getFirstName().isBlank()) {
                user.setFirstName(userDTO.getFirstName());
            }
            if (userDTO.getLastName() != null && !userDTO.getLastName().isBlank()) {
                user.setLastName(userDTO.getLastName());
            }
            if (userDTO.getProfilePhoto() != null && !userDTO.getProfilePhoto().isBlank()) {
                user.setProfilePhoto(userDTO.getProfilePhoto());
            }

            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateKeyException("Update error: " + e.getMessage());
        }
    }

    public void deleteUser(String submitMessage) {
        User user = getPrincipal();

        final String submitPhrase = "Delete my account forever!";
        if (!submitMessage.equals(submitPhrase)) {
            throw new CancellationException("The deletion is aborted.");
        }
        userRepository.deleteById(user.getId());
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public User getUserByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new NoResultException("No user found with username: " + username));
    }

    public List<User> getUsersByPrefix(String user_prefix) {
        return userRepository.findUsersByUsernameStartsWith(user_prefix);
    }
}
