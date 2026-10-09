package com.gridmind.security;

import com.gridmind.domain.UserRole;
import com.gridmind.entity.UserAccountEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record GridMindPrincipal(
        String id,
        String displayName,
        String email,
        String passwordHash,
        UserRole role) implements UserDetails {

    public static GridMindPrincipal from(UserAccountEntity account) {
        return new GridMindPrincipal(
                account.getId(),
                account.getDisplayName(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getRole()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
