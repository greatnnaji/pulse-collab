package com.pulse.service;

import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
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

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .avatarUrl(request.getAvatarUrl())
                .createdBy(currentUserId)
                .build();

        Group savedGroup = groupRepository.save(group);

        GroupMember creatorMembership = GroupMember.builder()
                .group(savedGroup)
                .user(currentUser)
                .role(GroupMember.MemberRole.ADMIN)
                .build();
        groupMemberRepository.save(creatorMembership);

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

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this group");
        }

        return toResponse(group);
    }

    private GroupResponse toResponse(Group group) {
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .createdBy(group.getCreatedBy())
                .createdAt(group.getCreatedAt())
                .memberCount(Math.toIntExact(groupMemberRepository.countByGroupId(group.getId())))
                .build();
    }
}
