package com.pulse.service;

import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
import com.pulse.entity.Group;
import com.pulse.entity.User;
import com.pulse.repository.GroupMemberRepository;
import com.pulse.repository.GroupRepository;
import com.pulse.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    @Test
    void createGroup_defaultsVisibilityToPublic() {
        Long currentUserId = 1L;
        User currentUser = User.builder().id(currentUserId).build();
        CreateGroupRequest request = CreateGroupRequest.builder()
                .name("Engineering")
                .description("Team group")
                .avatarUrl("https://example.com/group.png")
                .build();

        Group savedGroup = Group.builder()
                .id(10L)
                .name("Engineering")
                .description("Team group")
                .avatarUrl("https://example.com/group.png")
                .visibility(Group.Visibility.PUBLIC)
                .createdBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMemberRepository.countByGroupId(savedGroup.getId())).thenReturn(1L);

        GroupResponse response = groupService.createGroup(request, currentUserId);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());
        assertEquals(Group.Visibility.PUBLIC, groupCaptor.getValue().getVisibility());
        assertEquals(Group.Visibility.PUBLIC, response.getVisibility());
    }

    @Test
    void getGroupById_blocksNonMemberForPrivateGroup() {
        Long groupId = 2L;
        Long currentUserId = 99L;

        Group privateGroup = Group.builder()
                .id(groupId)
                .name("Private group")
                .visibility(Group.Visibility.PRIVATE)
                .createdBy(1L)
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(privateGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.getGroupById(groupId, currentUserId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are not a member of this private group", exception.getReason());
    }

    @Test
    void getGroupById_allowsNonMemberForPublicGroup() {
        Long groupId = 3L;
        Long currentUserId = 42L;

        Group publicGroup = Group.builder()
                .id(groupId)
                .name("Public group")
                .visibility(Group.Visibility.PUBLIC)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(publicGroup));
        when(groupMemberRepository.countByGroupId(groupId)).thenReturn(2L);

        GroupResponse response = groupService.getGroupById(groupId, currentUserId);

        assertEquals(groupId, response.getId());
        assertEquals(Group.Visibility.PUBLIC, response.getVisibility());
        assertEquals(2, response.getMemberCount());
    }
}
