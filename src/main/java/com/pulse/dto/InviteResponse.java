package com.pulse.dto;

import com.pulse.entity.GroupInvite;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private Long invitedBy;
    private LocalDateTime invitedAt;

    public static InviteResponse from(GroupInvite invite) {
        return InviteResponse.builder()
                .id(invite.getId())
                .groupId(invite.getGroup().getId())
                .groupName(invite.getGroup().getName())
                .invitedBy(invite.getInvitedBy())
                .invitedAt(invite.getInvitedAt())
                .build();
    }
}
