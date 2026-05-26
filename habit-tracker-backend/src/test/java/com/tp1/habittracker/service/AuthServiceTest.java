package com.tp1.habittracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.user.CreateUserRequest;
import com.tp1.habittracker.dto.user.LoginRequest;
import com.tp1.habittracker.exception.DuplicateResourceException;
import com.tp1.habittracker.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, userService);
    }

    @Test
    void registerHashesPasswordAndSavesUser() {
        CreateUserRequest request = new CreateUserRequest("  Manu  ", "  USER@Example.COM  ", "  myPassword123  ");
        UUID generatedId = UUID.randomUUID();

        when(userRepository.existsByUsernameIgnoreCase("Manu")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("myPassword123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(generatedId);
            return toSave;
        });

        User created = authService.register(request);

        assertEquals(generatedId, created.getId());
        assertEquals("Manu", created.getUsername());
        assertEquals("user@example.com", created.getEmail());
        assertEquals("$2a$10$encodedPassword", created.getPassword());
        assertNotEquals("myPassword123", created.getPassword());
        verify(passwordEncoder).encode("myPassword123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerThrowsWhenUsernameAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("manu", "manu@example.com", "Password123");
        when(userRepository.existsByUsernameIgnoreCase("manu")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void registerThrowsWhenUsernameAlreadyExistsEvenWithoutPassword() {
        CreateUserRequest request = new CreateUserRequest("manu", "manu@example.com", null);
        when(userRepository.existsByUsernameIgnoreCase("manu")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void registerThrowsWhenEmailAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("manu", "manu@example.com", "Password123");
        when(userRepository.existsByUsernameIgnoreCase("manu")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("manu@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void registerThrowsWhenPasswordIsBlank() {
        CreateUserRequest request = new CreateUserRequest("manu", "manu@example.com", "   ");

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("manu")
                .email("manu@example.com")
                .password("$2a$10$encoded")
                .build();
        LoginRequest request = new LoginRequest("  MANU@Example.com ", "password123");

        when(userRepository.findByEmail("manu@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        String token = authService.login(request);

        assertEquals("jwt-token", token);
        verify(userRepository).findByEmail("manu@example.com");
        verify(passwordEncoder).matches("password123", "$2a$10$encoded");
        verify(jwtService).generateToken(user);
    }

    @Test
    void loginThrowsWhenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("manu")
                .email("manu@example.com")
                .password("$2a$10$encoded")
                .build();
        LoginRequest request = new LoginRequest("manu@example.com", "wrong-password");

        when(userRepository.findByEmail("manu@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "$2a$10$encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
}