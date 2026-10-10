package com.pulse.service;

import com.pulse.dto.AuthResponse;
import com.pulse.dto.LoginRequest;
import com.pulse.dto.RegisterRequest;
import com.pulse.entity.AuditLogEventType;
import com.pulse.entity.Group;
import com.pulse.entity.GroupMember;
import com.pulse.entity.User;
import com.pulse.repository.GroupMemberRepository;
import com.pulse.repository.GroupRepository;
import com.pulse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

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
        verify(groupMemberRepository, never()).save(any());
    }

    @Test
    void register_joinsDemoGroupWhenConfigured() {
        RegisterRequest request = stubSuccessfulRegister();
        Group demoGroup = Group.builder().id(7L).name("Product Team").build();
        ReflectionTestUtils.setField(authService, "demoGroupId", 7L);
        when(groupRepository.findById(7L)).thenReturn(Optional.of(demoGroup));

        authService.register(request);

        ArgumentCaptor<GroupMember> captor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(captor.capture());
        assertEquals(demoGroup, captor.getValue().getGroup());
        assertEquals("alice", captor.getValue().getUser().getUsername());
        assertEquals(GroupMember.MemberRole.MEMBER, captor.getValue().getRole());
        verify(auditLogService).record(eq(AuditLogEventType.GROUP_MEMBER_ADDED), any(), any(), any(), any(), any(), any());
    }

    @Test
    void register_skipsDemoGroupWhenItDoesNotExist() {
        RegisterRequest request = stubSuccessfulRegister();
        ReflectionTestUtils.setField(authService, "demoGroupId", 7L);
        when(groupRepository.findById(7L)).thenReturn(Optional.empty());

        AuthResponse response = authService.register(request);

        assertEquals("token", response.getToken());
        verify(groupMemberRepository, never()).save(any());
    }

    private RegisterRequest stubSuccessfulRegister() {
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
        return request;
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