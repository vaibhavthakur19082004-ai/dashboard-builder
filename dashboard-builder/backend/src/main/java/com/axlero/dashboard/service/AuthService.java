package com.axlero.dashboard.service;

import com.axlero.dashboard.dto.AuthDtos.*;
import com.axlero.dashboard.entity.Role;
import com.axlero.dashboard.entity.User;
import com.axlero.dashboard.entity.Workspace;
import com.axlero.dashboard.exception.ConflictException;
import com.axlero.dashboard.repository.UserRepository;
import com.axlero.dashboard.repository.WorkspaceRepository;
import com.axlero.dashboard.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, WorkspaceRepository workspaces, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.workspaces = workspaces;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Registering creates a brand-new tenant workspace with the caller as its ADMIN. */
    public AuthResponse register(RegisterRequest r) {
        if (users.existsByUsername(r.username())) throw new ConflictException("Username already taken");
        Workspace ws = new Workspace();
        ws.setName(r.workspaceName());
        workspaces.save(ws);
        return response(newUser(r.username(), r.password(), Role.ADMIN, ws));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByUsername(r.username()).orElseThrow(() -> new BadCredentialsException("bad credentials"));
        if (!encoder.matches(r.password(), u.getPassword())) throw new BadCredentialsException("bad credentials");
        return response(u);
    }

    /** ADMIN invites a teammate into the same workspace. */
    public void createUser(CreateUserRequest r, User admin) {
        if (users.existsByUsername(r.username())) throw new ConflictException("Username already taken");
        newUser(r.username(), r.password(), r.role(), admin.getWorkspace());
    }

    private User newUser(String username, String password, Role role, Workspace ws) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setRole(role);
        u.setWorkspace(ws);
        return users.save(u);
    }

    private AuthResponse response(User u) {
        return new AuthResponse(jwt.generate(u), u.getUsername(), u.getRole(), u.getWorkspace().getId());
    }
}
