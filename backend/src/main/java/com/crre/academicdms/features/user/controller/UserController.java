package com.crre.academicdms.features.user.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.auth.dto.UserSummaryDto;
import com.crre.academicdms.features.user.dto.CreateUserRequest;
import com.crre.academicdms.features.user.dto.UpdateUserRequest;
import com.crre.academicdms.features.user.service.UserService;
import com.crre.academicdms.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getAllUsers(
        @RequestParam(required = false) String role,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        List<UserSummaryDto> users = userService.getAllUsers(role, caller);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @PostMapping("/students")
    public ResponseEntity<ApiResponse<UserSummaryDto>> createStudentUser(
        @Valid @RequestBody com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.CreateStudentUserRequest request,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        UserSummaryDto created = userService.createStudentUser(request, caller);
        return ResponseEntity.ok(ApiResponse.success("Student user created successfully", created));
    }

    @PostMapping("/faculty")
    public ResponseEntity<ApiResponse<UserSummaryDto>> createFacultyUser(
        @Valid @RequestBody com.crre.academicdms.features.user.dto.ScopeAwareUserDtos.CreateFacultyUserRequest request,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        UserSummaryDto created = userService.createFacultyUser(request, caller);
        return ResponseEntity.ok(ApiResponse.success("Faculty user created successfully", created));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserSummaryDto>> createUser(
        @Valid @RequestBody CreateUserRequest request,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        UserSummaryDto created = userService.createUser(request, caller);
        return ResponseEntity.ok(ApiResponse.success("User created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSummaryDto>> updateUser(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateUserRequest request,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        UserSummaryDto updated = userService.updateUser(id, request, caller);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
        @PathVariable UUID id,
        @RequestBody Map<String, Boolean> body,
        @AuthenticationPrincipal UserPrincipal caller
    ) {
        boolean enabled = body.getOrDefault("enabled", true);
        userService.toggleUserStatus(id, enabled, caller);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully to " + (enabled ? "enabled" : "disabled")));
    }
}
