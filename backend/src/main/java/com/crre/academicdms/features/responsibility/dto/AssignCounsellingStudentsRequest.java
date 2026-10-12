package com.crre.academicdms.features.responsibility.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record AssignCounsellingStudentsRequest(
    @NotEmpty(message = "At least one student must be selected")
    List<UUID> studentIds
) {}
