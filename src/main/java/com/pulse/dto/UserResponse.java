package com.pulse.dto;

import com.pulse.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private User.UserStatus status;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;

    // TODO: Add method to convert User entity to UserResponse DTO
}
