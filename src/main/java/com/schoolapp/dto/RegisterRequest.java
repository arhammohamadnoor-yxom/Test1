package com.schoolapp.dto;

import com.schoolapp.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Role is required")
    private User.UserRole role;

    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Student ID can only contain letters, numbers, and hyphens")
    private String studentId;

    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Staff ID can only contain letters, numbers, and hyphens")
    private String staffId;

    @Pattern(regexp = "^[+]?[0-9-()\\s]+$", message = "Please enter a valid phone number")
    private String phoneNumber;
}