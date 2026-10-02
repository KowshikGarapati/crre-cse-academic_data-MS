package com.crre.academicdms.features.auth.dto;

import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;

import java.util.Collections;
import java.util.List;
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
    Long apaarNumber,
    boolean enabled,
    List<ResponsibilitySummaryDto> responsibilities
) {
    public static UserSummaryDto fromEntity(User user) {
        return fromEntity(user, Collections.emptyList());
    }

    public static UserSummaryDto fromEntity(User user, List<ResponsibilitySummaryDto> responsibilities) {
        return new UserSummaryDto(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.getRegistrationNumber(),
            user.getUniqueId(),
            user.getAadharNumber(),
            user.getApaarNumber(),
            user.isEnabled(),
            responsibilities != null ? responsibilities : Collections.emptyList()
        );
    }
}
