package com.crre.academicdms.security;

import com.crre.academicdms.features.responsibility.repository.ResponsibilityRepository;
import com.crre.academicdms.features.user.model.User;
import com.crre.academicdms.features.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final ResponsibilityRepository responsibilityRepository;

    public CustomUserDetailsService(UserRepository userRepository, ResponsibilityRepository responsibilityRepository) {
        this.userRepository = userRepository;
        this.responsibilityRepository = responsibilityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByIdentifier(identifier)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

        boolean hasActiveHod = responsibilityRepository.existsByUserIdAndTypeAndStatus(
            user.getId(), "HOD_DEPT", "ACTIVE"
        );
        return UserPrincipal.create(user, hasActiveHod);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));

        boolean hasActiveHod = responsibilityRepository.existsByUserIdAndTypeAndStatus(
            user.getId(), "HOD_DEPT", "ACTIVE"
        );
        return UserPrincipal.create(user, hasActiveHod);
    }
}
