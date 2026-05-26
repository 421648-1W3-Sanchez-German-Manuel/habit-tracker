package com.tp1.habittracker.controller;

import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.user.CreateUserRequest;
import com.tp1.habittracker.dto.user.LoginRequest;
import com.tp1.habittracker.dto.user.LoginResponse;
import com.tp1.habittracker.dto.user.UserResponse;
import com.tp1.habittracker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return new LoginResponse(token);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId().toString(), user.getUsername(), user.getEmail());
    }
}