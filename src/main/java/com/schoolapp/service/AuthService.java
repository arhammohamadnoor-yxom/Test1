package com.schoolapp.service;

import com.schoolapp.dto.LoginRequest;
import com.schoolapp.dto.RegisterRequest;
import com.schoolapp.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserSessionService userSessionService;

    @Transactional
    public User login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();

            String sessionToken = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(8);

            userSessionService.createSession(user.getId(), sessionToken, expiresAt);

            return user;
        } catch (Exception e) {
            throw new RuntimeException("Invalid email or password", e);
        }
    }

    @Transactional
    public User register(RegisterRequest registerRequest) {
        User user = User.builder()
                .email(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .role(registerRequest.getRole())
                .studentId(registerRequest.getStudentId())
                .staffId(registerRequest.getStaffId())
                .phoneNumber(registerRequest.getPhoneNumber())
                .build();

        return userService.createUser(user);
    }

    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            userSessionService.deactivateAllSessionsForUser(user.getId());
        }
        SecurityContextHolder.clearContext();
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("No authenticated user found");
    }

    public boolean isCurrentUser(User user) {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getId().equals(user.getId());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasRole(User.UserRole role) {
        try {
            User currentUser = getCurrentUser();
            return currentUser.getRole() == role;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAdmin() {
        return hasRole(User.UserRole.ADMINISTRATOR);
    }

    public boolean isTeacher() {
        return hasRole(User.UserRole.TEACHER);
    }

    public boolean isStudent() {
        return hasRole(User.UserRole.STUDENT);
    }

    public boolean canAccessUserData(UUID targetUserId) {
        try {
            User currentUser = getCurrentUser();

            if (currentUser.getRole() == User.UserRole.ADMINISTRATOR) {
                return true;
            }

            return currentUser.getId().equals(targetUserId);
        } catch (Exception e) {
            return false;
        }
    }
}