package com.pulse.service;

import com.pulse.dto.AddMemberRequest;
import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
import com.pulse.dto.InviteResponse;
import com.pulse.entity.Group;
import com.pulse.entity.GroupInvite;
import com.pulse.entity.GroupMember;
import com.pulse.entity.User;
import com.pulse.repository.GroupInviteRepository;
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
import java.util.List;
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
    private GroupInviteRepository groupInviteRepository;

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
    void inviteMemberToGroup_allowsAdmin() {
        Long groupId = 1L;
        Long currentUserId = 10L;
        Long targetUserId = 20L;

        Group group = Group.builder().id(groupId).name("Engineering").createdBy(999L).build();
        GroupMember actingMember = GroupMember.builder()
            .role(GroupMember.MemberRole.ADMIN)
            .user(User.builder().id(currentUserId).username("admin").build())
            .build();
        User targetUser = User.builder().id(targetUserId).email("new@pulse.com").build();
        AddMemberRequest request = AddMemberRequest.builder().email("new@pulse.com").build();

        GroupInvite savedInvite = GroupInvite.builder()
                .id(100L)
                .group(group)
                .invitedUser(targetUser)
                .invitedBy(currentUserId)
                .invitedAt(LocalDateTime.now())
                .build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.of(actingMember));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(targetUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)).thenReturn(false);
        when(groupInviteRepository.existsByGroupIdAndInvitedUserId(groupId, targetUserId)).thenReturn(false);
        when(groupInviteRepository.save(any(GroupInvite.class))).thenReturn(savedInvite);

        InviteResponse response = groupService.inviteMemberToGroup(groupId, request, currentUserId);

        ArgumentCaptor<GroupInvite> inviteCaptor = ArgumentCaptor.forClass(GroupInvite.class);
        verify(groupInviteRepository).save(inviteCaptor.capture());
        assertEquals(targetUser, inviteCaptor.getValue().getInvitedUser());
        assertEquals(currentUserId, inviteCaptor.getValue().getInvitedBy());
        assertEquals(group, inviteCaptor.getValue().getGroup());
        assertEquals(groupId, response.getGroupId());

        ArgumentCaptor<com.pulse.entity.AuditLogEventType> eventCaptor =
                ArgumentCaptor.forClass(com.pulse.entity.AuditLogEventType.class);
        verify(auditLogService).record(eventCaptor.capture(), any(), any(), any(), any(), any(), any());
        assertEquals(com.pulse.entity.AuditLogEventType.GROUP_INVITE_CREATED, eventCaptor.getValue());
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
    }

    @Test
    void inviteMemberToGroup_blocksNonOwnerOrAdmin() {
        Long groupId = 1L;
        Long currentUserId = 10L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMember = GroupMember.builder().role(GroupMember.MemberRole.MEMBER).build();
        AddMemberRequest request = AddMemberRequest.builder().email("new@pulse.com").build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.of(actingMember));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.inviteMemberToGroup(groupId, request, currentUserId));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only group owner or admin can invite members", exception.getReason());
        verify(groupInviteRepository, never()).save(any(GroupInvite.class));
    }

    @Test
    void inviteMemberToGroup_returnsNotFoundWhenEmailUnknown() {
        Long groupId = 1L;
        Long currentUserId = 10L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMember = GroupMember.builder().role(GroupMember.MemberRole.ADMIN).build();
        AddMemberRequest request = AddMemberRequest.builder().email("nobody@pulse.com").build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.of(actingMember));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.inviteMemberToGroup(groupId, request, currentUserId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User with email not found", exception.getReason());
        verify(groupInviteRepository, never()).save(any(GroupInvite.class));
    }

    @Test
    void inviteMemberToGroup_blocksWhenAlreadyMember() {
        Long groupId = 1L;
        Long currentUserId = 10L;
        Long targetUserId = 20L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMember = GroupMember.builder().role(GroupMember.MemberRole.ADMIN).build();
        User targetUser = User.builder().id(targetUserId).email("existing@pulse.com").build();
        AddMemberRequest request = AddMemberRequest.builder().email("existing@pulse.com").build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.of(actingMember));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(targetUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.inviteMemberToGroup(groupId, request, currentUserId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("User is already a member of this group", exception.getReason());
        verify(groupInviteRepository, never()).existsByGroupIdAndInvitedUserId(any(), any());
        verify(groupInviteRepository, never()).save(any(GroupInvite.class));
    }

    @Test
    void inviteMemberToGroup_blocksWhenAlreadyInvited() {
        Long groupId = 1L;
        Long currentUserId = 10L;
        Long targetUserId = 20L;

        Group group = Group.builder().id(groupId).build();
        GroupMember actingMember = GroupMember.builder().role(GroupMember.MemberRole.ADMIN).build();
        User targetUser = User.builder().id(targetUserId).email("pending@pulse.com").build();
        AddMemberRequest request = AddMemberRequest.builder().email("pending@pulse.com").build();

        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)).thenReturn(Optional.of(actingMember));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(targetUser));
        when(groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)).thenReturn(false);
        when(groupInviteRepository.existsByGroupIdAndInvitedUserId(groupId, targetUserId)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.inviteMemberToGroup(groupId, request, currentUserId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("User has already been invited to this group", exception.getReason());
        verify(groupInviteRepository, never()).save(any(GroupInvite.class));
    }

    @Test
    void getMyInvites_mapsGroupAndInviteFields() {
        Long currentUserId = 5L;

        Group group = Group.builder().id(3L).name("Design").build();
        GroupInvite invite = GroupInvite.builder()
                .id(7L)
                .group(group)
                .invitedUser(User.builder().id(currentUserId).build())
                .invitedBy(1L)
                .invitedAt(LocalDateTime.now())
                .build();

        when(groupInviteRepository.findByInvitedUserIdWithGroup(currentUserId)).thenReturn(List.of(invite));

        List<InviteResponse> responses = groupService.getMyInvites(currentUserId);

        assertEquals(1, responses.size());
        assertEquals(7L, responses.get(0).getId());
        assertEquals(3L, responses.get(0).getGroupId());
        assertEquals("Design", responses.get(0).getGroupName());
        assertEquals(1L, responses.get(0).getInvitedBy());
    }

    @Test
    void acceptInvite_createsMembershipAndDeletesInvite() {
        Long inviteId = 7L;
        Long currentUserId = 5L;

        Group group = Group.builder().id(3L).name("Design").build();
        User invitedUser = User.builder().id(currentUserId).username("invitee").build();
        GroupInvite invite = GroupInvite.builder()
                .id(inviteId)
                .group(group)
                .invitedUser(invitedUser)
                .invitedBy(1L)
                .build();
        GroupMember savedMember = GroupMember.builder()
                .id(50L)
                .group(group)
                .user(invitedUser)
                .role(GroupMember.MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        when(groupInviteRepository.findByIdAndInvitedUserId(inviteId, currentUserId)).thenReturn(Optional.of(invite));
        when(groupMemberRepository.save(any(GroupMember.class))).thenReturn(savedMember);

        groupService.acceptInvite(inviteId, currentUserId);

        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        assertEquals(GroupMember.MemberRole.MEMBER, memberCaptor.getValue().getRole());
        assertEquals(invitedUser, memberCaptor.getValue().getUser());
        verify(groupInviteRepository).delete(invite);

        ArgumentCaptor<com.pulse.entity.AuditLogEventType> eventCaptor =
                ArgumentCaptor.forClass(com.pulse.entity.AuditLogEventType.class);
        verify(auditLogService).record(eventCaptor.capture(), any(), any(), any(), any(), any(), any());
        assertEquals(com.pulse.entity.AuditLogEventType.GROUP_INVITE_ACCEPTED, eventCaptor.getValue());
    }

    @Test
    void acceptInvite_returnsNotFoundWhenInviteMissingOrNotOwned() {
        Long inviteId = 7L;
        Long currentUserId = 5L;

        when(groupInviteRepository.findByIdAndInvitedUserId(inviteId, currentUserId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.acceptInvite(inviteId, currentUserId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Invite not found", exception.getReason());
        verify(groupMemberRepository, never()).save(any(GroupMember.class));
        verify(groupInviteRepository, never()).delete(any(GroupInvite.class));
    }

    @Test
    void declineInvite_deletesInviteRow() {
        Long inviteId = 7L;
        Long currentUserId = 5L;

        Group group = Group.builder().id(3L).name("Design").build();
        User invitedUser = User.builder().id(currentUserId).username("invitee").build();
        GroupInvite invite = GroupInvite.builder()
                .id(inviteId)
                .group(group)
                .invitedUser(invitedUser)
                .invitedBy(1L)
                .build();

        when(groupInviteRepository.findByIdAndInvitedUserId(inviteId, currentUserId)).thenReturn(Optional.of(invite));

        groupService.declineInvite(inviteId, currentUserId);

        verify(groupInviteRepository).delete(invite);
        verify(groupMemberRepository, never()).save(any(GroupMember.class));

        ArgumentCaptor<com.pulse.entity.AuditLogEventType> eventCaptor =
                ArgumentCaptor.forClass(com.pulse.entity.AuditLogEventType.class);
        verify(auditLogService).record(eventCaptor.capture(), any(), any(), any(), any(), any(), any());
        assertEquals(com.pulse.entity.AuditLogEventType.GROUP_INVITE_DECLINED, eventCaptor.getValue());
    }

    @Test
    void declineInvite_returnsNotFoundWhenInviteMissingOrNotOwned() {
        Long inviteId = 7L;
        Long currentUserId = 5L;

        when(groupInviteRepository.findByIdAndInvitedUserId(inviteId, currentUserId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> groupService.declineInvite(inviteId, currentUserId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Invite not found", exception.getReason());
        verify(groupInviteRepository, never()).delete(any(GroupInvite.class));
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
