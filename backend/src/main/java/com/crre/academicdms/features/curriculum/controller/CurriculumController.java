package com.crre.academicdms.features.curriculum.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.curriculum.dto.CurriculumDtos.*;
import com.crre.academicdms.features.curriculum.service.CurriculumService;
import com.crre.academicdms.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CurriculumController {

    private final CurriculumService curriculumService;

    public CurriculumController(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    @GetMapping("/curriculum/subjects/{subjectId}/units")
    public ResponseEntity<ApiResponse<SubjectCurriculumDto>> getCurriculumForSubject(
        @PathVariable UUID subjectId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        SubjectCurriculumDto curriculum = curriculumService.getCurriculumForSubject(subjectId, caller);
        return ResponseEntity.ok(ApiResponse.success("Curriculum units retrieved successfully", curriculum));
    }

    @GetMapping("/student/resources")
    public ResponseEntity<ApiResponse<List<SubjectCurriculumDto>>> getStudentResources(
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<SubjectCurriculumDto> list = curriculumService.getStudentCurriculumResources(caller);
        return ResponseEntity.ok(ApiResponse.success("Student resources retrieved successfully", list));
    }

    @PostMapping("/curriculum/concepts/{conceptId}/resources")
    public ResponseEntity<ApiResponse<ResourceDto>> createResource(
        @PathVariable UUID conceptId,
        @Valid @RequestBody CreateResourceRequest req,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        ResourceDto created = curriculumService.createResource(conceptId, req, caller);
        return ResponseEntity.ok(ApiResponse.success("Learning resource created successfully", created));
    }
}
