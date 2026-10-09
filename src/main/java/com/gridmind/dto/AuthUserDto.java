package com.gridmind.dto;

import com.gridmind.security.GridMindPrincipal;

public record AuthUserDto(String id, String displayName, String email, String role) {

    public static AuthUserDto from(GridMindPrincipal principal) {
        return new AuthUserDto(
                principal.id(),
                principal.displayName(),
                principal.email(),
                principal.role().name()
        );
    }
}
