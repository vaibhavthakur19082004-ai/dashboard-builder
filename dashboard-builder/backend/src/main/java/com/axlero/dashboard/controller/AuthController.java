package com.axlero.dashboard.controller;

import com.axlero.dashboard.dto.AuthDtos.*;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        return auth.register(r);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r);
    }

    @PostMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(@Valid @RequestBody CreateUserRequest r, @AuthenticationPrincipal User admin) {
        auth.createUser(r, admin);
    }
}
