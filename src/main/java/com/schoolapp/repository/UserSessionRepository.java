package com.schoolapp.repository;

import com.schoolapp.model.User;
import com.schoolapp.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findBySessionToken(String sessionToken);

    List<UserSession> findByUser(User user);

    List<UserSession> findByUserId(UUID userId);

    List<UserSession> findByIsActive(Boolean isActive);

    List<UserSession> findByExpiresAtBefore(LocalDateTime expiryTime);

    @Query("SELECT us FROM UserSession us WHERE us.sessionToken = :token AND us.isActive = true AND us.expiresAt > :now")
    Optional<UserSession> findValidSession(@Param("token") String sessionToken, @Param("now") LocalDateTime now);

    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.isActive = true ORDER BY us.expiresAt DESC")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.id = :userId AND us.isActive = true")
    long countActiveSessionsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.user.id = :userId")
    void deactivateAllSessionsForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.expiresAt < :now")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserSession us WHERE us.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT us FROM UserSession us WHERE us.isActive = true AND us.expiresAt < :now")
    List<UserSession> findExpiredActiveSessions(@Param("now") LocalDateTime now);
}