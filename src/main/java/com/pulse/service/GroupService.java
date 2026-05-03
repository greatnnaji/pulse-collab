package com.pulse.service;

import com.pulse.dto.AddMemberRequest;
import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
import com.pulse.dto.MemberResponse;
import com.pulse.entity.AuditLogEventType;
import com.pulse.entity.Group;
import com.pulse.entity.GroupMember;
import com.pulse.entity.User;
import com.pulse.repository.GroupMemberRepository;
import com.pulse.repository.GroupRepository;
import com.pulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .avatarUrl(request.getAvatarUrl())
                .visibility(request.getVisibility() == null ? Group.Visibility.PUBLIC : request.getVisibility())
                .createdBy(currentUserId)
                .build();

        Group savedGroup = groupRepository.save(group);

        GroupMember creatorMembership = GroupMember.builder()
                .group(savedGroup)
                .user(currentUser)
            .role(GroupMember.MemberRole.OWNER)
                .build();
        groupMemberRepository.save(creatorMembership);
        auditLogService.record(
            AuditLogEventType.GROUP_CREATED,
            currentUserId,
            currentUser.getUsername(),
            "GROUP",
            savedGroup.getId(),
            savedGroup.getName(),
            "Group created"
        );

        return toResponse(savedGroup);
    }

    public List<GroupResponse> getUserGroups(Long userId) {
        List<GroupMember> memberships = groupMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(GroupMember::getGroup)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    public GroupResponse getGroupById(Long groupId, Long currentUserId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // All groups now require membership (visibility ignored as short-term enforcement)
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this group");
        }

        return toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getGroupMembers(Long groupId, Long currentUserId) {
        // Verify group exists
        if (!groupRepository.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }

        // Verify current user is a member of the group
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this group");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupIdWithUser(groupId);
        return members.stream()
                .map(MemberResponse::from)
                .toList();
    }

    @Transactional
    public MemberResponse addMemberToGroup(Long groupId, AddMemberRequest request, Long currentUserId) {
        // Verify group exists
        Group group = getExistingGroup(groupId);

        GroupMember actingMembership = getExistingMembership(groupId, currentUserId);
        if (!canManageMembers(actingMembership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group owner or admin can add members");
        }

        // Find user by email
        User userToAdd = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with email not found"));

        // Check if user is already a member
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userToAdd.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this group");
        }

        // Add user to group as MEMBER
        GroupMember newMember = GroupMember.builder()
                .group(group)
                .user(userToAdd)
                .role(GroupMember.MemberRole.MEMBER)
                .build();

        GroupMember savedMember = groupMemberRepository.save(newMember);
        auditLogService.record(
            AuditLogEventType.GROUP_MEMBER_ADDED,
            currentUserId,
            actingMembership.getUser().getUsername(),
            "GROUP",
            groupId,
            group.getName(),
            "Added user " + userToAdd.getId()
        );
        return MemberResponse.from(savedMember);
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    @Transactional
    public void removeMemberFromGroup(Long groupId, Long memberUserId, Long actingUserId) {
        Group group = getExistingGroup(groupId);

        GroupMember actingMembership = getExistingMembership(groupId, actingUserId);
        if (!canManageMembers(actingMembership.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only group owner or admin can remove members");
        }

        if (actingUserId.equals(memberUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use leave group operation to remove your own membership");
        }

        GroupMember membershipToRemove = getExistingMembership(groupId, memberUserId);
        if (membershipToRemove.getRole() == GroupMember.MemberRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner cannot be removed from the group");
        }

        groupMemberRepository.delete(membershipToRemove);
        auditLogService.record(
            AuditLogEventType.GROUP_MEMBER_REMOVED,
            actingUserId,
            actingMembership.getUser().getUsername(),
            "GROUP",
            groupId,
            group.getName(),
            "Removed user " + memberUserId
        );
    }

    @Transactional
    public void leaveGroup(Long groupId, Long currentUserId) {
        Group group = getExistingGroup(groupId);

        GroupMember membership = getExistingMembership(groupId, currentUserId);
        if (membership.getRole() == GroupMember.MemberRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Owner cannot leave group before transferring ownership");
        }

        groupMemberRepository.delete(membership);
        auditLogService.record(
            AuditLogEventType.GROUP_LEFT,
            currentUserId,
            membership.getUser().getUsername(),
            "GROUP",
            groupId,
            group.getName(),
            "User left the group"
        );
    }

    private Group getExistingGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private GroupMember getExistingMembership(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this group"));
    }

    private boolean canManageMembers(GroupMember.MemberRole role) {
        return role == GroupMember.MemberRole.OWNER || role == GroupMember.MemberRole.ADMIN;
    }

    private GroupResponse toResponse(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .visibility(group.getVisibility())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .memberCount(Math.toIntExact(groupMemberRepository.countByGroupId(group.getId())))
                .build();
    }
}
