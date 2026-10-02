package com.crre.academicdms.features.user.service;

import com.crre.academicdms.features.auth.dto.UserSummaryDto;
import com.crre.academicdms.features.responsibility.dto.ResponsibilityRequest;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.service.ResponsibilityService;
import com.crre.academicdms.features.user.dto.CreateUserRequest;
import com.crre.academicdms.features.user.dto.UpdateUserRequest;
import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ResponsibilityService responsibilityService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        ResponsibilityService responsibilityService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.responsibilityService = responsibilityService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDto> getAllUsers(UserPrincipal caller) {
        enforceAdminOrHod(caller);

        List<User> users = userRepository.findAll();
        return users.stream()
            .map(u -> {
                List<ResponsibilitySummaryDto> resps = responsibilityService.getActiveResponsibilitiesForUser(u.getId());
                return UserSummaryDto.fromEntity(u, resps);
            })
            .toList();
    }

    @Transactional
    public UserSummaryDto createUser(CreateUserRequest req, UserPrincipal caller) {
        enforceDownwardAuthority(caller, req.role());

        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists: " + req.username());
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists: " + req.email());
        }

        User user = new User(
            req.username().trim(),
            req.email().trim(),
            passwordEncoder.encode(req.password()),
            req.fullName().trim(),
            req.role()
        );
        user.setRegistrationNumber(req.registrationNumber());
        user.setUniqueId(req.uniqueId());
        user.setAadharNumber(req.aadharNumber());
        user.setApaarNumber(req.apaarNumber());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);

        // Process initial role-specific assignments if provided
        if (req.role() == Role.FACULTY) {
            if (req.subjectId() != null && req.sectionId() != null) {
                responsibilityService.addResponsibility(
                    savedUser.getId(),
                    new ResponsibilityRequest("TEACHING", req.departmentId(), req.programId(), req.subjectId(), req.sectionId(), null)
                );
            }
            if (req.classTeacher() && req.sectionId() != null) {
                responsibilityService.addResponsibility(
                    savedUser.getId(),
                    new ResponsibilityRequest("CLASS_TEACHER", req.departmentId(), req.programId(), null, req.sectionId(), null)
                );
            }
        } else if (req.role() == Role.HOD && req.departmentId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("HOD_DEPT", req.departmentId(), req.programId(), null, null, null)
            );
        } else if (req.role() == Role.COUNSELLOR && req.sectionId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("COUNSELLOR", req.departmentId(), req.programId(), null, req.sectionId(), null)
            );
        } else if (req.role() == Role.CLASS_TEACHER && req.sectionId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("CLASS_TEACHER", req.departmentId(), req.programId(), null, req.sectionId(), null)
            );
        } else if (req.role() == Role.PRINCIPAL && req.departmentId() != null) {
            responsibilityService.addResponsibility(
                savedUser.getId(),
                new ResponsibilityRequest("PRINCIPAL_INSTITUTION", req.departmentId(), null, null, null, null)
            );
        }

        List<ResponsibilitySummaryDto> resps = responsibilityService.getActiveResponsibilitiesForUser(savedUser.getId());
        return UserSummaryDto.fromEntity(savedUser, resps);
    }

    @Transactional
    public UserSummaryDto updateUser(UUID userId, UpdateUserRequest req, UserPrincipal caller) {
        enforceDownwardAuthority(caller, req.role());

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        user.setFullName(req.fullName().trim());
        user.setEmail(req.email().trim());
        user.setRole(req.role());
        user.setRegistrationNumber(req.registrationNumber());
        user.setUniqueId(req.uniqueId());
        user.setAadharNumber(req.aadharNumber());
        user.setApaarNumber(req.apaarNumber());
        user.setEnabled(req.enabled());

        User saved = userRepository.save(user);
        List<ResponsibilitySummaryDto> resps = responsibilityService.getActiveResponsibilitiesForUser(saved.getId());
        return UserSummaryDto.fromEntity(saved, resps);
    }

    @Transactional
    public void toggleUserStatus(UUID userId, boolean enabled, UserPrincipal caller) {
        enforceAdminOrHod(caller);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        if (caller.getRole() == Role.HOD && (user.getRole() == Role.PRINCIPAL || user.getRole() == Role.ADMIN)) {
            throw new AccessDeniedException("HOD cannot modify a Principal or Admin account");
        }

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    private void enforceAdminOrHod(UserPrincipal caller) {
        if (caller == null) {
            throw new AccessDeniedException("Authentication required");
        }
        Role role = caller.getRole();
        if (role != Role.ADMIN && role != Role.PRINCIPAL && role != Role.HOD) {
            throw new AccessDeniedException("Access denied: You do not have administrative authority");
        }
    }

    private void enforceDownwardAuthority(UserPrincipal caller, Role targetRole) {
        enforceAdminOrHod(caller);
        if (caller.getRole() == Role.HOD) {
            if (targetRole == Role.PRINCIPAL || targetRole == Role.ADMIN) {
                throw new AccessDeniedException("HOD cannot create or assign a Principal or Admin");
            }
        }
    }
}
