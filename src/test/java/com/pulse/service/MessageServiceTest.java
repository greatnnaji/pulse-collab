package com.pulse.service;

import com.pulse.dto.CreateMessageRequest;
import com.pulse.dto.MessageResponse;
import com.pulse.entity.Group;
import com.pulse.entity.Message;
import com.pulse.entity.User;
import com.pulse.repository.GroupMemberRepository;
import com.pulse.repository.GroupRepository;
import com.pulse.repository.MessageRepository;
import com.pulse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

        @Mock
        private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    @Test
    void createMessage_succeeds_forMember() {
        Long currentUserId = 1L;
        Long groupId = 10L;
        String content = "Hello team!";

        User sender = User.builder().id(currentUserId).username("alice").build();
        Group group = Group.builder().id(groupId).name("Engineering").build();
        CreateMessageRequest request = CreateMessageRequest.builder().content(content).build();

        Message savedMessage = Message.builder()
                .id(100L)
                .sender(sender)
                .group(group)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(sender));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        MessageResponse response = messageService.createMessage(request, currentUserId, groupId);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(content, response.getContent());
        assertEquals(currentUserId, response.getSenderId());
        assertEquals(groupId, response.getGroupId());
        verify(messageRepository).save(any(Message.class));
                verify(messagingTemplate).convertAndSend(eq("/topic/groups/" + groupId), any(MessageResponse.class));
    }

    @Test
    void createMessage_fails_forNonMember() {
        Long currentUserId = 1L;
        Long groupId = 10L;

        User sender = User.builder().id(currentUserId).username("alice").build();
        Group group = Group.builder().id(groupId).name("Engineering").build();
        CreateMessageRequest request = CreateMessageRequest.builder().content("Hello").build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(sender));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> messageService.createMessage(request, currentUserId, groupId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_fails_userNotFound() {
        Long currentUserId = 999L;
        Long groupId = 10L;

        CreateMessageRequest request = CreateMessageRequest.builder().content("Hello").build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> messageService.createMessage(request, currentUserId, groupId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_fails_groupNotFound() {
        Long currentUserId = 1L;
        Long groupId = 999L;

        User sender = User.builder().id(currentUserId).username("alice").build();
        CreateMessageRequest request = CreateMessageRequest.builder().content("Hello").build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(sender));
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> messageService.createMessage(request, currentUserId, groupId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getMessagesByGroup_succeeds_forMember() {
        Long currentUserId = 1L;
        Long groupId = 10L;

        Group group = Group.builder().id(groupId).name("Engineering").build();

        User sender1 = User.builder().id(1L).username("alice").build();
        User sender2 = User.builder().id(2L).username("bob").build();

        Message msg1 = Message.builder()
                .id(101L)
                .sender(sender2)
                .group(group)
                .content("Second message")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        Message msg2 = Message.builder()
                .id(102L)
                .sender(sender1)
                .group(group)
                .content("Latest message")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Message> messagesPage = new PageImpl<>(List.of(msg2, msg1), PageRequest.of(0, 20), 2);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(true);
        when(messageRepository.findByGroupId(eq(groupId), any(Pageable.class)))
                .thenReturn(messagesPage);

        Page<MessageResponse> response = messageService.getMessagesByGroup(groupId, currentUserId, 0, 20);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        // Verify newest-first ordering (msg2 created after msg1)
        assertEquals("Latest message", response.getContent().get(0).getContent());
        assertEquals("Second message", response.getContent().get(1).getContent());
        assertEquals(2, response.getTotalElements());
    }

    @Test
    void getMessagesByGroup_fails_forNonMember() {
        Long currentUserId = 1L;
        Long groupId = 10L;

        Group group = Group.builder().id(groupId).name("Engineering").build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> messageService.getMessagesByGroup(groupId, currentUserId, 0, 20));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(messageRepository, never()).findByGroupId(any(), any());
    }

    @Test
    void getMessagesByGroup_fails_groupNotFound() {
        Long currentUserId = 1L;
        Long groupId = 999L;

        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> messageService.getMessagesByGroup(groupId, currentUserId, 0, 20));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(messageRepository, never()).findByGroupId(any(), any());
    }

    @Test
    void getMessagesByGroup_returnsPaginatedResults() {
        Long currentUserId = 1L;
        Long groupId = 10L;

        Group group = Group.builder().id(groupId).name("Engineering").build();
        User sender = User.builder().id(1L).username("alice").build();

        Message msg = Message.builder()
                .id(101L)
                .sender(sender)
                .group(group)
                .content("Test message")
                .createdAt(LocalDateTime.now())
                .build();

        Page<Message> messagesPage = new PageImpl<>(List.of(msg), PageRequest.of(0, 10), 1);

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(true);
        when(messageRepository.findByGroupId(eq(groupId), any(Pageable.class)))
                .thenReturn(messagesPage);

        Page<MessageResponse> response = messageService.getMessagesByGroup(groupId, currentUserId, 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
        assertEquals(0, response.getNumber());
        verify(messageRepository).findByGroupId(eq(groupId), any(Pageable.class));
    }
}
