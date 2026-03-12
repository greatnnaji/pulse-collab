package com.pulse.controller;

import com.pulse.dto.AuthResponse;
import com.pulse.dto.LoginRequest;
import com.pulse.dto.RegisterRequest;
import com.pulse.dto.UserResponse;
import com.pulse.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // TODO: Implement register endpoint
        // Call authService.register() and return response
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // TODO: Implement login endpoint
        // Call authService.login() and return response
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        // TODO: Implement get current user endpoint
        // 1. Extract userId from JWT token (via SecurityContextHolder or @AuthenticationPrincipal)
        // 2. Call authService.getCurrentUser(userId)
        // 3. Return user details
        throw new UnsupportedOperationException("Get current user endpoint not implemented yet");
    }
}
