package com.tp1.habittracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.user.CreateUserRequest;
import com.tp1.habittracker.exception.ResourceNotFoundException;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUserNormalizesFieldsAndSavesUser() {
        CreateUserRequest request = new CreateUserRequest("  Manu  ", "  USER@Example.COM  ", "  Secret123  ");
        UUID generatedId = UUID.randomUUID();

        when(userRepository.existsByUsernameIgnoreCase("Manu")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User toSave = invocation.getArgument(0);
            toSave.setId(generatedId);
            return toSave;
        });

        User created = userService.createUser(request);

        assertEquals(generatedId, created.getId());
        assertEquals("Manu", created.getUsername());
        assertEquals("user@example.com", created.getEmail());
        assertEquals("$2a$10$encodedPassword", created.getPassword());
        assertNotEquals("Secret123", created.getPassword());
        verify(userRepository).existsByUsernameIgnoreCase("Manu");
        verify(userRepository).existsByEmailIgnoreCase("user@example.com");
        verify(passwordEncoder).encode("Secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getUserByIdThrowsWhenUserDoesNotExist() {
        UUID missingUserId = UUID.randomUUID();
        when(userRepository.findById(missingUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(missingUserId.toString()));
    }
}
