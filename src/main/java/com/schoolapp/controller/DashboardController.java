package com.schoolapp.controller;

import com.schoolapp.model.User;
import com.schoolapp.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AuthService authService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            switch (currentUser.getRole()) {
                case STUDENT:
                    return "redirect:/student/dashboard";
                case TEACHER:
                    return "redirect:/teacher/dashboard";
                case ADMINISTRATOR:
                    return "redirect:/admin/dashboard";
                default:
                    model.addAttribute("error", "Invalid user role");
                    return "error/access-denied";
            }
        } catch (Exception e) {
            return "redirect:/login";
        }
    }
}