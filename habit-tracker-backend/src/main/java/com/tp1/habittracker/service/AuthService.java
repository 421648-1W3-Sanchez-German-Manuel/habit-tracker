package com.tp1.habittracker.service;

import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.user.CreateUserRequest;
import com.tp1.habittracker.dto.user.LoginRequest;
import com.tp1.habittracker.repository.UserRepository;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public User register(CreateUserRequest request) {
        return userService.createUser(request);
    }

    public String login(LoginRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        String rawPassword = request.password();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return jwtService.generateToken(user);
    }
}