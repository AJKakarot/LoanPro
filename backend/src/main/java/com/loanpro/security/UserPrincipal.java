package com.loanpro.security;

import com.loanpro.modules.identity.domain.User;
import com.loanpro.modules.identity.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final boolean locked;
    private final int tokenVersion;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            UUID id,
            String email,
            String password,
            boolean enabled,
            boolean locked,
            int tokenVersion,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.locked = locked;
        this.tokenVersion = tokenVersion;
        this.authorities = authorities;
    }

    public static UserPrincipal from(User user) {
        var authorities = user.roleNames().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
        boolean enabled = user.getStatus() == UserStatus.ACTIVE && !locked;
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                enabled,
                locked,
                user.getTokenVersion(),
                authorities
        );
    }

    public UUID getId() {
        return id;
    }

    public int getTokenVersion() {
        return tokenVersion;
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
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
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
