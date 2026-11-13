package com.axlero.dashboard.dto;

import com.axlero.dashboard.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 100) String workspaceName) { }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }

    public record CreateUserRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotNull Role role) { }

    public record AuthResponse(String token, String username, Role role, Long workspaceId) { }
}
