package com.pulse.service;

import com.pulse.dto.AuthResponse;
import com.pulse.dto.LoginRequest;
import com.pulse.dto.RegisterRequest;
import com.pulse.dto.UserResponse;
import com.pulse.entity.AuditLogEventType;
import com.pulse.entity.User;
import com.pulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .status(User.UserStatus.ONLINE)
                .lastSeenAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        auditLogService.record(
            AuditLogEventType.USER_REGISTERED,
            savedUser.getId(),
            savedUser.getUsername(),
            "USER",
            savedUser.getId(),
            savedUser.getUsername(),
            "User registered"
        );
        String token = jwtService.generateToken(savedUser.getId(), savedUser.getUsername());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getDisplayName()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.record(
                    AuditLogEventType.USER_LOGIN_FAILED,
                    null,
                    request.getUsernameOrEmail(),
                    "USER",
                    null,
                    request.getUsernameOrEmail(),
                    "Invalid credentials"
            );
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastSeenAt(LocalDateTime.now());
        user.setStatus(User.UserStatus.ONLINE);
        userRepository.save(user);
        auditLogService.record(
            AuditLogEventType.USER_LOGIN_SUCCESS,
            user.getId(),
            user.getUsername(),
            "USER",
            user.getId(),
            user.getUsername(),
            "User logged in"
        );

        String token = jwtService.generateToken(user.getId(), user.getUsername());

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName()
        );
    }

    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return UserResponse.from(user);
    }
}
