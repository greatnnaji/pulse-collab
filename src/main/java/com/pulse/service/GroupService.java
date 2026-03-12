package com.pulse.service;

import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
import com.pulse.entity.Group;
import com.pulse.entity.GroupMember;
import com.pulse.repository.GroupMemberRepository;
import com.pulse.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public GroupResponse createGroup(CreateGroupRequest request, Long currentUserId) {
        // TODO: Implement create group
        // 1. Create Group entity from request
        // 2. Set createdBy to currentUserId
        // 3. Save group
        // 4. Add creator as ADMIN member in GroupMember table
        // 5. Convert to GroupResponse and return
        throw new UnsupportedOperationException("Create group not implemented yet");
    }

    public List<GroupResponse> getUserGroups(Long userId) {
        // TODO: Implement get user groups
        // 1. Find all GroupMember records for userId
        // 2. Get associated Group entities
        // 3. Convert to List<GroupResponse> with member counts
        // 4. Return list
        throw new UnsupportedOperationException("Get user groups not implemented yet");
    }

    public GroupResponse getGroupById(Long groupId, Long currentUserId) {
        // TODO: Implement get group by ID
        // 1. Find group by ID
        // 2. Verify currentUserId is a member of the group
        // 3. Convert to GroupResponse with member details
        // 4. Return group details
        throw new UnsupportedOperationException("Get group by ID not implemented yet");
    }
}
