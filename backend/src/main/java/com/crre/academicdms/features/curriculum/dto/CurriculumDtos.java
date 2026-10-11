package com.crre.academicdms.features.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CurriculumDtos {

    public record ResourceDto(
        UUID id,
        UUID conceptId,
        String title,
        String description,
        String resourceType, // YOUTUBE_VIDEO, PDF_DOCUMENT
        String url,
        Long fileSizeBytes,
        String uploadedByName,
        Instant createdAt
    ) {}

    public record ConceptDto(
        UUID id,
        UUID unitId,
        int conceptOrder,
        String title,
        String description,
        List<ResourceDto> resources
    ) {}

    public record UnitDto(
        UUID id,
        UUID subjectId,
        int unitNumber,
        String title,
        String description,
        List<ConceptDto> concepts
    ) {}

    public record SubjectCurriculumDto(
        UUID subjectId,
        String subjectCode,
        String subjectName,
        String type,
        BigDecimal credits,
        String facultyInstructor,
        List<UnitDto> units
    ) {}

    public record CreateResourceRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotBlank(message = "Resource type is required") String resourceType,
        @NotBlank(message = "URL is required") String url,
        Long fileSizeBytes
    ) {}
}
