package com.crre.academicdms.features.auth.service;

import com.crre.academicdms.features.auth.dto.AuthResponse;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.auth.dto.UserSummaryDto;
import com.crre.academicdms.features.responsibility.dto.ResponsibilitySummaryDto;
import com.crre.academicdms.features.responsibility.service.ResponsibilityService;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.crre.academicdms.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserRepository userRepository;
    private final ResponsibilityService responsibilityService;

    public AuthService(
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        UserRepository userRepository,
        ResponsibilityService responsibilityService
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userRepository = userRepository;
        this.responsibilityService = responsibilityService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.identifier() != null ? request.identifier().trim() : "",
                request.password() != null ? request.password().trim() : ""
            )
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + principal.getId()));

        List<ResponsibilitySummaryDto> responsibilities = responsibilityService.getActiveResponsibilitiesForUser(user.getId());

        return AuthResponse.of(UserSummaryDto.fromEntity(user, responsibilities));
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new UsernameNotFoundException("No authenticated user in session");
        }

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + principal.getId()));

        List<ResponsibilitySummaryDto> responsibilities = responsibilityService.getActiveResponsibilitiesForUser(user.getId());

        return UserSummaryDto.fromEntity(user, responsibilities);
    }
}
