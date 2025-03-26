package com.sashaprylutsky.wishplus.service;

import com.sashaprylutsky.wishplus.model.User;
import com.sashaprylutsky.wishplus.model.UserPrincipal;
import com.sashaprylutsky.wishplus.repository.UserRepository;
import jakarta.persistence.NoResultException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    public UserService(BCryptPasswordEncoder encoder, UserRepository userRepository) {
        this.encoder = encoder;
        this.userRepository = userRepository;
    }

    private UserPrincipal getUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
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
        User userRecord = userRepository.findById(id)
                .orElseThrow(() -> new NoResultException("No user found with ID " + id));

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

        String submitPhrase = "Delete your account forever!";

        if (!Objects.equals(userPrincipal.getId(), userRecord.getId())) {
            throw new AccessDeniedException("Access to delete is denied.");
        }
        if (!submitMessage.equals(submitPhrase)) {
            throw new CancellationException("The deletion is aborted.");
        }

        userRepository.delete(userRecord);
    }
}
