package com.pulse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMemberRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
