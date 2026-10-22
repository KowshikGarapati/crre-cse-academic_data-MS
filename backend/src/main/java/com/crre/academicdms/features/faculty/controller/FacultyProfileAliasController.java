package com.crre.academicdms.features.faculty.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.FacultyProfileDto;
import com.crre.academicdms.features.faculty.dto.FacultyDtos.UpdateFacultyProfileRequest;
import com.crre.academicdms.features.faculty.service.FacultyService;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Alias REST controller for `/api/v1/faculty-profiles` ensuring complete backwards
 * compatibility alongside `/api/v1/faculty/profiles`.
 */
@RestController
@RequestMapping("/api/v1/faculty-profiles")
public class FacultyProfileAliasController {

    private final FacultyService facultyService;

    public FacultyProfileAliasController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FacultyProfileDto>>> getProfiles(
        @RequestParam(required = false) UUID departmentId,
        @RequestParam(required = false, defaultValue = "true") Boolean includeSynthetic,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<FacultyProfileDto> profiles = facultyService.getFacultyProfiles(departmentId, includeSynthetic, caller);
        return ResponseEntity.ok(ApiResponse.success("Faculty profiles retrieved successfully", profiles));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ApiResponse<FacultyProfileDto>> getProfileById(
        @PathVariable UUID profileId,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        FacultyProfileDto profile = facultyService.getFacultyProfileById(profileId, caller);
        return ResponseEntity.ok(ApiResponse.success("Faculty profile retrieved successfully", profile));
    }

    @PutMapping("/{profileId}")
    public ResponseEntity<ApiResponse<FacultyProfileDto>> updateProfile(
        @PathVariable UUID profileId,
        @RequestBody UpdateFacultyProfileRequest req,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        FacultyProfileDto updated = facultyService.updateFacultyProfile(profileId, req, caller);
        return ResponseEntity.ok(ApiResponse.success("Faculty profile updated successfully", updated));
    }
}
