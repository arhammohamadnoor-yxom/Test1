package com.schoolapp.controller;

import com.schoolapp.dto.LoginRequest;
import com.schoolapp.dto.RegisterRequest;
import com.schoolapp.model.User;
import com.schoolapp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @GetMapping("/")
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                               @RequestParam(value = "logout", required = false) String logout,
                               @RequestParam(value = "expired", required = false) String expired,
                               Model model) {

        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }

        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }

        if (expired != null) {
            model.addAttribute("message", "Your session has expired, please login again");
        }

        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest loginRequest,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes,
                       HttpSession session) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fill in all required fields correctly");
            return "redirect:/login";
        }

        try {
            User user = authService.login(loginRequest);
            session.setAttribute("currentUser", user);

            String redirectUrl = getDashboardUrlForRole(user.getRole());
            log.info("User {} logged in successfully, redirecting to {}", user.getEmail(), redirectUrl);

            return "redirect:" + redirectUrl;

        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Invalid email or password");
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("roles", User.UserRole.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest registerRequest,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fill in all required fields correctly");
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registerRequest", bindingResult);
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }

        if (registerRequest.getRole() == User.UserRole.STUDENT &&
            (registerRequest.getStudentId() == null || registerRequest.getStudentId().trim().isEmpty())) {
            redirectAttributes.addFlashAttribute("error", "Student ID is required for students");
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }

        if ((registerRequest.getRole() == User.UserRole.TEACHER || registerRequest.getRole() == User.UserRole.ADMINISTRATOR) &&
            (registerRequest.getStaffId() == null || registerRequest.getStaffId().trim().isEmpty())) {
            redirectAttributes.addFlashAttribute("error", "Staff ID is required for teachers and administrators");
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }

        try {
            User user = authService.register(registerRequest);
            log.info("New user registered successfully: {}", user.getEmail());
            redirectAttributes.addFlashAttribute("message", "Registration successful! Please login with your credentials.");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("Registration failed for user: {}", registerRequest.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("registerRequest", registerRequest);
            return "redirect:/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        try {
            authService.logout();
            request.getSession().invalidate();
            redirectAttributes.addFlashAttribute("message", "You have been logged out successfully");
            log.info("User logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed", e);
            redirectAttributes.addFlashAttribute("error", "Logout failed");
        }
        return "redirect:/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }

    private String getDashboardUrlForRole(User.UserRole role) {
        switch (role) {
            case STUDENT:
                return "/student/dashboard";
            case TEACHER:
                return "/teacher/dashboard";
            case ADMINISTRATOR:
                return "/admin/dashboard";
            default:
                return "/dashboard";
        }
    }
}