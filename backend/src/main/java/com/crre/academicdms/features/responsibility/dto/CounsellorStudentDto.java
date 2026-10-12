package com.crre.academicdms.features.responsibility.dto;

import java.time.Instant;
import java.util.UUID;

public record CounsellorStudentDto(
    UUID assignmentId,
    UUID studentId,
    String registrationNumber,
    String fullName,
    String entryType,
    String sectionName,
    String programName,
    String semesterName,
    Instant assignedAt,
    String status
) {}
