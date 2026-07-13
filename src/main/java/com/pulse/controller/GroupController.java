package com.pulse.controller;

import com.pulse.dto.AddMemberRequest;
import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
import com.pulse.dto.InviteResponse;
import com.pulse.dto.MemberResponse;
import com.pulse.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        Long currentUserId = extractCurrentUserId();
        GroupResponse created = groupService.createGroup(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups() {
        Long currentUserId = extractCurrentUserId();
        return ResponseEntity.ok(groupService.getUserGroups(currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id) {
        Long currentUserId = extractCurrentUserId();
        return ResponseEntity.ok(groupService.getGroupById(id, currentUserId));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<MemberResponse>> getGroupMembers(@PathVariable Long groupId) {
        Long currentUserId = extractCurrentUserId();
        return ResponseEntity.ok(groupService.getGroupMembers(groupId, currentUserId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<InviteResponse> inviteMemberToGroup(@PathVariable Long groupId,
                                                              @Valid @RequestBody AddMemberRequest request) {
        Long currentUserId = extractCurrentUserId();
        InviteResponse invite = groupService.inviteMemberToGroup(groupId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(invite);
    }

    @DeleteMapping("/{groupId}/members/{memberUserId}")
    public ResponseEntity<Void> removeMemberFromGroup(@PathVariable Long groupId, @PathVariable Long memberUserId) {
        Long currentUserId = extractCurrentUserId();
        groupService.removeMemberFromGroup(groupId, memberUserId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long groupId) {
        Long currentUserId = extractCurrentUserId();
        groupService.leaveGroup(groupId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    private Long extractCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }

        if (principal instanceof String str && !str.isBlank() && !"anonymousUser".equals(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
    }
}
