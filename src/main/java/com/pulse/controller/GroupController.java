package com.pulse.controller;

import com.pulse.dto.CreateGroupRequest;
import com.pulse.dto.GroupResponse;
import com.pulse.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        // TODO: Implement create group endpoint
        // 1. Extract currentUserId from JWT token (via SecurityContextHolder or @AuthenticationPrincipal)
        // 2. Call groupService.createGroup(request, currentUserId)
        // 3. Return created group with HTTP 201 status
        throw new UnsupportedOperationException("Create group endpoint not implemented yet");
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups() {
        // TODO: Implement get user groups endpoint
        // 1. Extract currentUserId from JWT token
        // 2. Call groupService.getUserGroups(currentUserId)
        // 3. Return list of groups user is member of
        throw new UnsupportedOperationException("Get user groups endpoint not implemented yet");
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupById(@PathVariable Long id) {
        // TODO: Implement get group by ID endpoint
        // 1. Extract currentUserId from JWT token
        // 2. Call groupService.getGroupById(id, currentUserId)
        // 3. Return group details if user is a member
        throw new UnsupportedOperationException("Get group by ID endpoint not implemented yet");
    }
}
