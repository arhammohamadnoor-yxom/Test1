package com.schoolapp.service;

import com.schoolapp.model.User;
import com.schoolapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }

        if (user.getRole() == User.UserRole.STUDENT && user.getStudentId() != null) {
            if (userRepository.existsByStudentId(user.getStudentId())) {
                throw new RuntimeException("Student ID already exists: " + user.getStudentId());
            }
        }

        if ((user.getRole() == User.UserRole.TEACHER || user.getRole() == User.UserRole.ADMINISTRATOR)
            && user.getStaffId() != null) {
            if (userRepository.existsByStaffId(user.getStaffId())) {
                throw new RuntimeException("Staff ID already exists: " + user.getStaffId());
            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsActive(true);

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(UUID userId, User userDetails) {
        User user = getUserById(userId);

        if (!user.getEmail().equals(userDetails.getEmail()) &&
            userRepository.existsByEmail(userDetails.getEmail())) {
            throw new RuntimeException("Email already exists: " + userDetails.getEmail());
        }

        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setEmail(userDetails.getEmail());
        user.setPhoneNumber(userDetails.getPhoneNumber());

        if (userDetails.getStudentId() != null && !userDetails.getStudentId().equals(user.getStudentId())) {
            if (userRepository.existsByStudentId(userDetails.getStudentId())) {
                throw new RuntimeException("Student ID already exists: " + userDetails.getStudentId());
            }
            user.setStudentId(userDetails.getStudentId());
        }

        if (userDetails.getStaffId() != null && !userDetails.getStaffId().equals(user.getStaffId())) {
            if (userRepository.existsByStaffId(userDetails.getStaffId())) {
                throw new RuntimeException("Staff ID already exists: " + userDetails.getStaffId());
            }
            user.setStaffId(userDetails.getStaffId());
        }

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        userSessionService.deactivateAllSessionsForUser(userId);
    }

    @Transactional
    public void deactivateUser(UUID userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);
        userSessionService.deactivateAllSessionsForUser(userId);
    }

    @Transactional
    public void activateUser(UUID userId) {
        User user = getUserById(userId);
        user.setIsActive(true);
        userRepository.save(user);
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByStudentId(String studentId) {
        return userRepository.findByStudentId(studentId);
    }

    public Optional<User> findByStaffId(String staffId) {
        return userRepository.findByStaffId(staffId);
    }

    public List<User> getAllActiveUsers() {
        return userRepository.findActiveUsers();
    }

    public List<User> getUsersByRole(User.UserRole role) {
        return userRepository.findByRoleAndIsActive(role, true);
    }

    public List<User> searchUsers(String search) {
        return userRepository.searchUsers(search);
    }

    public long countUsersByRole(User.UserRole role) {
        return userRepository.countActiveUsersByRole(role);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean studentIdExists(String studentId) {
        return userRepository.existsByStudentId(studentId);
    }

    public boolean staffIdExists(String staffId) {
        return userRepository.existsByStaffId(staffId);
    }
}