package com.crre.academicdms.features.auth.controller;

import com.crre.academicdms.common.ApiResponse;
import com.crre.academicdms.features.auth.dto.AuthResponse;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.auth.dto.UserSummaryDto;
import com.crre.academicdms.features.auth.service.AuthService;
import com.crre.academicdms.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        AuthResponse authResponse = authService.login(loginRequest, request, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummaryDto>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserSummaryDto userSummary = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully", userSummary));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
