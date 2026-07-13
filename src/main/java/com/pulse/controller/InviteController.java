package com.pulse.controller;

import com.pulse.dto.InviteResponse;
import com.pulse.dto.MemberResponse;
import com.pulse.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final GroupService groupService;

    @GetMapping("/me")
    public ResponseEntity<List<InviteResponse>> getMyInvites() {
        Long currentUserId = extractCurrentUserId();
        return ResponseEntity.ok(groupService.getMyInvites(currentUserId));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<MemberResponse> acceptInvite(@PathVariable Long id) {
        Long currentUserId = extractCurrentUserId();
        MemberResponse member = groupService.acceptInvite(id, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> declineInvite(@PathVariable Long id) {
        Long currentUserId = extractCurrentUserId();
        groupService.declineInvite(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // duplicated from GroupController/AuthController by convention - not in scope to refactor
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
