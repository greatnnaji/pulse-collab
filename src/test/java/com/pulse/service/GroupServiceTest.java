package com.pulse.service;

import com.pulse.dto.AddMemberRequest;
import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
import com.pulse.entity.Group;
import com.pulse.entity.GroupMember;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private GroupService groupService;

    @Test
    void createGroup_mapsFieldsAndCreatesOwnerMembership() {
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
                .createdBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMemberRepository.countByGroupId(savedGroup.getId())).thenReturn(1L);

        GroupResponse response = groupService.createGroup(request, currentUserId);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(groupCaptor.capture());
        assertEquals("Engineering", groupCaptor.getValue().getName());
        assertEquals("Team group", groupCaptor.getValue().getDescription());
        assertEquals("https://example.com/group.png", groupCaptor.getValue().getAvatarUrl());
        assertEquals("Engineering", response.getName());
        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(auditLogService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createGroup_assignsOwnerRoleToCreator() {
        Long currentUserId = 1L;
        User currentUser = User.builder().id(currentUserId).build();
        CreateGroupRequest request = CreateGroupRequest.builder().name("Engineering").build();

        Group savedGroup = Group.builder()
                .id(10L)
                .name("Engineering")
                .createdBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMemberRepository.countByGroupId(savedGroup.getId())).thenReturn(1L);

        groupService.createGroup(request, currentUserId);

        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        assertEquals(GroupMember.MemberRole.OWNER, memberCaptor.getValue().getRole());
    }

    @Test
    void getGroupById_blocksNonMemberForPrivateGroup() {
        Long groupId = 2L;
        Long currentUserId = 99L;

        Group privateGroup = Group.builder()
                .id(groupId)
                .name("Private group")
                .createdBy(1L)
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(privateGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.getGroupById(groupId, currentUserId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are not a member of this group", exception.getReason());
    }

    @Test
    void getGroupById_blocksNonMemberForPublicGroup() {
        Long groupId = 3L;
        Long currentUserId = 42L;

        Group publicGroup = Group.builder()
                .id(groupId)
                .name("Public group")
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(publicGroup));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.getGroupById(groupId, currentUserId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("You are not a member of this group", exception.getReason());
    }

    @Test
    void addMemberToGroup_allowsAdmin() {
        Long groupId = 1L;
        Long currentUserId = 10L;
        Long targetUserId = 20L;

        Group group = Group.builder().id(groupId).createdBy(999L).build();
        GroupMember actingMember = GroupMember.builder()
            .role(GroupMember.MemberRole.ADMIN)
            .user(User.builder().id(currentUserId).username("admin").build())
            .build();
        User targetUser = User.builder().id(targetUserId).email("new@pulse.com").build();
        AddMemberRequest request = AddMemberRequest.builder().email("new@pulse.com").build();

        GroupMember savedMember = GroupMember.builder()
                .id(100L)
                .group(group)
                .user(targetUser)
                .role(GroupMember.MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.of(actingMember));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(targetUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(savedMember);

        groupService.addMemberToGroup(groupId, request, currentUserId);

        verify(groupMemberRepository).save(any(GroupMember.class));
        verify(auditLogService).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void removeMemberFromGroup_blocksRemovingOwner() {
        Long groupId = 7L;
        Long actingUserId = 10L;
        Long ownerUserId = 1L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMember = GroupMember.builder().role(GroupMember.MemberRole.ADMIN).build();
        GroupMember ownerMember = GroupMember.builder().role(GroupMember.MemberRole.OWNER).build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actingUserId)).thenReturn(Optional.of(actingMember));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, ownerUserId)).thenReturn(Optional.of(ownerMember));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.removeMemberFromGroup(groupId, ownerUserId, actingUserId));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Owner cannot be removed from the group", exception.getReason());
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
        verify(auditLogService, never()).record(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void removeMemberFromGroup_blocksMemberWithoutPrivileges() {
        Long groupId = 10L;
        Long actingUserId = 2L;
        Long targetUserId = 3L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMembership = GroupMember.builder().role(GroupMember.MemberRole.MEMBER).build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actingUserId)).thenReturn(Optional.of(actingMembership));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.removeMemberFromGroup(groupId, targetUserId, actingUserId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only group owner or admin can remove members", exception.getReason());
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
    }

    @Test
    void removeMemberFromGroup_blocksRemovingSelf() {
        Long groupId = 11L;
        Long actingUserId = 4L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMembership = GroupMember.builder().role(GroupMember.MemberRole.ADMIN).build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actingUserId)).thenReturn(Optional.of(actingMembership));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.removeMemberFromGroup(groupId, actingUserId, actingUserId));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Use leave group operation to remove your own membership", exception.getReason());
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
    }

    @Test
    void removeMemberFromGroup_returnsNotFoundWhenGroupMissing() {
        Long groupId = 12L;

        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.removeMemberFromGroup(groupId, 9L, 1L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Group not found", exception.getReason());
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
    }

    @Test
    void removeMemberFromGroup_deletesTargetMemberWhenAuthorized() {
        Long groupId = 13L;
        Long actingUserId = 2L;
        Long targetUserId = 3L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMembership = GroupMember.builder()
            .role(GroupMember.MemberRole.ADMIN)
            .user(User.builder().id(actingUserId).username("admin").build())
            .build();
        GroupMember targetMembership = GroupMember.builder().role(GroupMember.MemberRole.MEMBER).build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, actingUserId)).thenReturn(Optional.of(actingMembership));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)).thenReturn(Optional.of(targetMembership));

        groupService.removeMemberFromGroup(groupId, targetUserId, actingUserId);

        verify(groupMemberRepository, times(1)).delete(targetMembership);
    }

    @Test
    void leaveGroup_blocksOwner() {
        Long groupId = 8L;
        Long ownerUserId = 1L;

        Group group = Group.builder().id(groupId).build();
        GroupMember ownerMembership = GroupMember.builder()
                .role(GroupMember.MemberRole.OWNER)
                .user(User.builder().id(ownerUserId).username("owner").build())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, ownerUserId)).thenReturn(Optional.of(ownerMembership));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.leaveGroup(groupId, ownerUserId));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Owner cannot leave group before transferring ownership", exception.getReason());
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
    }

    @Test
    void leaveGroup_deletesMembershipForNonOwner() {
        Long groupId = 9L;
        Long memberUserId = 2L;

        Group group = Group.builder().id(groupId).build();
        GroupMember member = GroupMember.builder()
                .role(GroupMember.MemberRole.MEMBER)
                .user(User.builder().id(memberUserId).username("member").build())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId)).thenReturn(Optional.of(member));

        groupService.leaveGroup(groupId, memberUserId);

        verify(groupMemberRepository, times(1)).delete(member);
    }

    @Test
    void leaveGroup_returnsNotFoundWhenGroupMissing() {
        Long groupId = 14L;

        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.leaveGroup(groupId, 2L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Group not found", exception.getReason());
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
    }

    @Test
    void leaveGroup_blocksNonMember() {
        Long groupId = 15L;
        Long currentUserId = 20L;

        Group group = Group.builder().id(groupId).build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.leaveGroup(groupId, currentUserId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Not a member of this group", exception.getReason());
        verify(groupMemberRepository, never()).delete(any(GroupMember.class));
    }
}
