package com.crre.academicdms.security;

import com.crre.academicdms.features.user.model.Role;
import com.crre.academicdms.features.user.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final String fullName;
    private final Role role;
    private final String registrationNumber;
    private final String uniqueId;
    private final Long aadharNumber;
    private final Long apaarNumber;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final User user;
    private final boolean hasActiveHodResponsibility;

    public UserPrincipal(
        UUID id,
        String username,
        String email,
        String password,
        String fullName,
        Role role,
        String registrationNumber,
        String uniqueId,
        Long aadharNumber,
        Long apaarNumber,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities,
        User user
    ) {
        this(id, username, email, password, fullName, role, registrationNumber, uniqueId, aadharNumber, apaarNumber, enabled, authorities, user, role == Role.HOD);
    }

    public UserPrincipal(
        UUID id,
        String username,
        String email,
        String password,
        String fullName,
        Role role,
        String registrationNumber,
        String uniqueId,
        Long aadharNumber,
        Long apaarNumber,
        boolean enabled,
        Collection<? extends GrantedAuthority> authorities,
        User user,
        boolean hasActiveHodResponsibility
    ) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.registrationNumber = registrationNumber;
        this.uniqueId = uniqueId;
        this.aadharNumber = aadharNumber;
        this.apaarNumber = apaarNumber;
        this.enabled = enabled;
        this.authorities = authorities;
        this.user = user;
        this.hasActiveHodResponsibility = hasActiveHodResponsibility;
    }

    public static UserPrincipal create(User user) {
        return create(user, user.getRole() == Role.HOD);
    }

    public static UserPrincipal create(User user, boolean hasActiveHodResponsibility) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        if (user.getRole() == Role.HOD || hasActiveHodResponsibility) {
            authorities.add(new SimpleGrantedAuthority("ROLE_HOD"));
        }

        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getPasswordHash(),
            user.getFullName(),
            user.getRole(),
            user.getRegistrationNumber(),
            user.getUniqueId(),
            user.getAadharNumber(),
            user.getApaarNumber(),
            user.isEnabled(),
            List.copyOf(authorities),
            user,
            hasActiveHodResponsibility
        );
    }

    public boolean isHod() {
        return this.role == Role.HOD || this.hasActiveHodResponsibility ||
            (this.authorities != null && this.authorities.stream().anyMatch(a -> "ROLE_HOD".equals(a.getAuthority())));
    }

    public boolean hasActiveHodResponsibility() {
        return hasActiveHodResponsibility;
    }

    public User getUser() {
        return user;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Long getAadharNumber() {
        return aadharNumber;
    }

    public Long getApaarNumber() {
        return apaarNumber;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
