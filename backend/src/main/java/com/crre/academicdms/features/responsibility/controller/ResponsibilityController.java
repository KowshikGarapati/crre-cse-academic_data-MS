package com.crre.academicdms.features.responsibility.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.service.ResponsibilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ResponsibilityController {

    private final ResponsibilityService responsibilityService;

    public ResponsibilityController(ResponsibilityService responsibilityService) {
        this.responsibilityService = responsibilityService;
    }

    @GetMapping("/users/{userId}/responsibilities")
    public ResponseEntity<ApiResponse<List<ResponsibilitySummaryDto>>> getUserResponsibilities(@PathVariable UUID userId) {
        List<ResponsibilitySummaryDto> list = responsibilityService.getActiveResponsibilitiesForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User responsibilities retrieved successfully", list));
    }

    @PostMapping("/users/{userId}/responsibilities")
    public ResponseEntity<ApiResponse<ResponsibilitySummaryDto>> addResponsibility(
        @PathVariable UUID userId,
        @Valid @RequestBody ResponsibilityRequest request
    ) {
        ResponsibilitySummaryDto created = responsibilityService.addResponsibility(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Responsibility assigned successfully", created));
    }

    @PutMapping("/responsibilities/{id}")
    public ResponseEntity<ApiResponse<ResponsibilitySummaryDto>> updateResponsibility(
        @PathVariable UUID id,
        @Valid @RequestBody ResponsibilityRequest request
    ) {
        ResponsibilitySummaryDto updated = responsibilityService.updateResponsibility(id, request);
        return ResponseEntity.ok(ApiResponse.success("Responsibility updated successfully", updated));
    }

    @DeleteMapping("/responsibilities/{id}")
    public ResponseEntity<ApiResponse<Void>> removeResponsibility(@PathVariable UUID id) {
        responsibilityService.removeResponsibility(id);
        return ResponseEntity.ok(ApiResponse.success("Responsibility removed successfully"));
    }
}
