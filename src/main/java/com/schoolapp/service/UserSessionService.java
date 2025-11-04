package com.schoolapp.service;

import com.schoolapp.model.UserSession;
import com.schoolapp.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    @Transactional
    public UserSession createSession(UUID userId, String sessionToken, LocalDateTime expiresAt) {
        UserSession session = UserSession.builder()
                .userId(userId)
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .isActive(true)
                .build();

        return userSessionRepository.save(session);
    }

    @Transactional
    public void deactivateSession(UUID sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        session.setIsActive(false);
        userSessionRepository.save(session);
    }

    @Transactional
    public void deactivateAllSessionsForUser(UUID userId) {
        userSessionRepository.deactivateAllSessionsForUser(userId);
    }

    @Transactional
    public void deactivateExpiredSessions() {
        userSessionRepository.deactivateExpiredSessions(LocalDateTime.now());
    }

    @Transactional
    public void cleanupExpiredSessions() {
        userSessionRepository.deleteExpiredSessions(LocalDateTime.now());
    }

    public Optional<UserSession> getValidSession(String sessionToken) {
        return userSessionRepository.findValidSession(sessionToken, LocalDateTime.now());
    }

    public List<UserSession> getActiveSessionsByUserId(UUID userId) {
        return userSessionRepository.findActiveSessionsByUserId(userId);
    }

    public long countActiveSessionsByUserId(UUID userId) {
        return userSessionRepository.countActiveSessionsByUserId(userId);
    }

    public List<UserSession> getExpiredSessions() {
        return userSessionRepository.findExpiredActiveSessions(LocalDateTime.now());
    }

    public boolean isSessionValid(String sessionToken) {
        return getValidSession(sessionToken).isPresent();
    }
}