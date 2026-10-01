package com.crre.academicdms.features.auth.dto;

import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;

import java.util.UUID;

public record UserSummaryDto(
    UUID id,
    String username,
    String email,
    String fullName,
    Role role,
    String registrationNumber,
    String uniqueId,
    Long aadharNumber,
    Long apaarNumber
) {
    public static UserSummaryDto fromEntity(User user) {
        return new UserSummaryDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getRegistrationNumber(),
            user.getUniqueId(),
            user.getAadharNumber(),
            user.getApaarNumber()
        );
    }
}
