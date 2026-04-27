package com.pulse.controller;

import com.pulse.service.GroupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupControllerTest {

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void removeMemberFromGroup_returnsNoContentAndCallsService() {
        setAuthenticatedUser(10L);

        ResponseEntity<Void> response = groupController.removeMemberFromGroup(7L, 5L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(groupService).removeMemberFromGroup(7L, 5L, 10L);
    }

    @Test
    void leaveGroup_returnsNoContentAndCallsService() {
        setAuthenticatedUser(12L);

        ResponseEntity<Void> response = groupController.leaveGroup(9L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(groupService).leaveGroup(9L, 12L);
    }

    @Test
    void removeMemberFromGroup_throwsUnauthorizedWithoutPrincipal() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupController.removeMemberFromGroup(7L, 5L));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getReason());
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
