package com.crre.academicdms.features.auth.service;

import com.crre.academicdms.features.auth.dto.AuthResponse;
import com.crre.academicdms.features.auth.dto.LoginRequest;
import com.crre.academicdms.features.auth.dto.UserSummaryDto;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import com.crre.academicdms.security.JwtTokenProvider;
import com.crre.academicdms.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthService(
        AuthenticationManager authenticationManager,
        JwtTokenProvider tokenProvider,
        UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.identifier().trim(),
                request.password()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(principal);

        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + principal.getId()));

        return AuthResponse.of(
            token,
            tokenProvider.getExpirationMs(),
            UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + principal.getId()));

        return UserSummaryDto.fromEntity(user);
    }
}
