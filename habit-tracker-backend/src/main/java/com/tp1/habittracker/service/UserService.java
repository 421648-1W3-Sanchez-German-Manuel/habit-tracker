package com.tp1.habittracker.service;

import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.user.CreateUserRequest;
import com.tp1.habittracker.exception.DuplicateResourceException;
import com.tp1.habittracker.exception.ResourceNotFoundException;
import com.tp1.habittracker.repository.UserRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(CreateUserRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        String normalizedUsername = normalize(request.username());
        String normalizedEmail = normalize(request.email()).toLowerCase(Locale.ROOT);

        if (normalizedUsername.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("Email already exists");
        }

        String rawPassword = normalize(request.password());
        if (rawPassword.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = User.builder()
            .username(normalizedUsername)
            .email(normalizedEmail)
            .password(passwordEncoder.encode(rawPassword))
            .build();

        return userRepository.save(user);
    }

    public User getUserById(String id) {
        String userId = Objects.requireNonNull(id, "id must not be null");
        UUID parsedUserId = parseUuidOrThrowNotFound(userId);
        return userRepository.findById(parsedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UUID parseUuidOrThrowNotFound(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
