package com.pulse.controller;

import com.pulse.dto.InviteResponse;
import com.pulse.dto.MemberResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteControllerTest {

    @Mock
    private GroupService groupService;

    @InjectMocks
    private InviteController inviteController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyInvites_returnsOkAndCallsService() {
        setAuthenticatedUser(10L);
        List<InviteResponse> invites = List.of(InviteResponse.builder().id(1L).build());
        when(groupService.getMyInvites(10L)).thenReturn(invites);

        ResponseEntity<List<InviteResponse>> response = inviteController.getMyInvites();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(invites, response.getBody());
        verify(groupService).getMyInvites(10L);
    }

    @Test
    void acceptInvite_returnsCreatedAndCallsService() {
        setAuthenticatedUser(10L);
        MemberResponse member = MemberResponse.builder().id(5L).build();
        when(groupService.acceptInvite(7L, 10L)).thenReturn(member);

        ResponseEntity<MemberResponse> response = inviteController.acceptInvite(7L);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(member, response.getBody());
        verify(groupService).acceptInvite(7L, 10L);
    }

    @Test
    void declineInvite_returnsNoContentAndCallsService() {
        setAuthenticatedUser(10L);

        ResponseEntity<Void> response = inviteController.declineInvite(7L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(groupService).declineInvite(7L, 10L);
    }

    @Test
    void getMyInvites_throwsUnauthorizedWithoutPrincipal() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> inviteController.getMyInvites());

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getReason());
    }

    private void setAuthenticatedUser(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null)
        );
    }
}
