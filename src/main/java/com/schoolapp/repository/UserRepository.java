package com.schoolapp.repository;

import com.schoolapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByStaffId(String staffId);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByStaffId(String staffId);

    List<User> findByRole(User.UserRole role);

    List<User> findByRoleAndIsActive(User.UserRole role, Boolean isActive);

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.firstName, u.lastName")
    List<User> findActiveUsers();

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true ORDER BY u.firstName, u.lastName")
    List<User> findActiveUsersByRole(@Param("role") User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.firstName ILIKE %:search% OR u.lastName ILIKE %:search% OR u.email ILIKE %:search%")
    List<User> searchUsers(@Param("search") String search);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = true")
    long countActiveUsersByRole(@Param("role") User.UserRole role);
}