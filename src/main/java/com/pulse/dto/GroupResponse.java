package com.pulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private String avatarUrl;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Integer memberCount;

    // TODO: Add method to convert Group entity to GroupResponse DTO
    // TODO: Add list of members when implementing member details
}
