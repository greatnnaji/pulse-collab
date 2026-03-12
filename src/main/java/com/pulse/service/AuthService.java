package com.pulse.service;

import com.pulse.dto.AuthResponse;
import com.pulse.dto.LoginRequest;
import com.pulse.dto.RegisterRequest;
import com.pulse.dto.UserResponse;
import com.pulse.entity.User;
import com.pulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // TODO: Implement user registration
        // 1. Check if username or email already exists
        // 2. Hash password using passwordEncoder
        // 3. Create and save User entity
        // 4. Generate JWT token using jwtService
        // 5. Return AuthResponse with token and user details
        throw new UnsupportedOperationException("Register endpoint not implemented yet");
    }

    public AuthResponse login(LoginRequest request) {
        // TODO: Implement user login
        // 1. Find user by username or email
        // 2. Verify password using passwordEncoder.matches()
        // 3. Generate JWT token using jwtService
        // 4. Update lastSeenAt timestamp
        // 5. Return AuthResponse with token and user details
        throw new UnsupportedOperationException("Login endpoint not implemented yet");
    }

    public UserResponse getCurrentUser(Long userId) {
        // TODO: Implement get current user
        // 1. Find user by ID from JWT token
        // 2. Convert User entity to UserResponse DTO
        // 3. Return user details (without password)
        throw new UnsupportedOperationException("Get current user not implemented yet");
    }
}
