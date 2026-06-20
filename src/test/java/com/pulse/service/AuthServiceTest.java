package com.pulse.service;

import com.pulse.dto.AuthResponse;
import com.pulse.dto.LoginRequest;
import com.pulse.dto.RegisterRequest;
import com.pulse.entity.User;
import com.pulse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_writesAuditLog() {
        RegisterRequest request = RegisterRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .password("password123")
                .displayName("Alice")
                .build();

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(1L, "alice")).thenReturn("token");

        AuthResponse response = authService.register(request);

        assertEquals("token", response.getToken());
        verify(auditLogService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void login_writesFailureAuditLogWhenCredentialsAreWrong() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("alice@example.com")
                .password("wrong")
                .build();
        User user = User.builder().id(1L).username("alice").password("hashed").build();

        when(userRepository.findByUsername(request.getUsernameOrEmail())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(request.getUsernameOrEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));

        assertEquals("Invalid credentials", exception.getReason());
        verify(auditLogService).recordIndependent(any(), any(), any(), any(), any(), any(), any());
        verify(jwtService, never()).generateToken(any(), any());
    }
}