package com.schoolapp.controller;

import com.schoolapp.model.User;
import com.schoolapp.model.Class;
import com.schoolapp.model.Room;
import com.schoolapp.model.AttendanceRecord;
import com.schoolapp.model.RoomBooking;
import com.schoolapp.service.AuthService;
import com.schoolapp.service.UserService;
import com.schoolapp.repository.UserRepository;
import com.schoolapp.repository.ClassRepository;
import com.schoolapp.repository.RoomRepository;
import com.schoolapp.repository.AttendanceRecordRepository;
import com.schoolapp.repository.RoomBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final RoomBookingRepository roomBookingRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            // System statistics
            long totalUsers = userRepository.count();
            long totalStudents = userRepository.countActiveUsersByRole(User.UserRole.STUDENT);
            long totalTeachers = userRepository.countActiveUsersByRole(User.UserRole.TEACHER);
            long totalAdmins = userRepository.countActiveUsersByRole(User.UserRole.ADMINISTRATOR);
            long totalClasses = classRepository.count();
            long totalRooms = roomRepository.count();
            long totalAttendanceRecords = attendanceRepository.count();
            long totalBookings = roomBookingRepository.count();

            // Today's statistics
            List<AttendanceRecord> todayAttendance = attendanceRepository.findByDate(LocalDate.now());
            long presentToday = todayAttendance.stream()
                    .filter(record -> record.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
                    .count();

            List<RoomBooking> currentBookings = roomBookingRepository.findCurrentBookings();

            model.addAttribute("user", currentUser);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("totalTeachers", totalTeachers);
            model.addAttribute("totalAdmins", totalAdmins);
            model.addAttribute("totalClasses", totalClasses);
            model.addAttribute("totalRooms", totalRooms);
            model.addAttribute("totalAttendanceRecords", totalAttendanceRecords);
            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("presentToday", presentToday);
            model.addAttribute("totalPresentToday", todayAttendance.size());
            model.addAttribute("currentBookings", currentBookings.size());

            return "admin/dashboard";
        } catch (Exception e) {
            log.error("Error loading admin dashboard", e);
            return "redirect:/login";
        }
    }

    @GetMapping("/users")
    public String users(@RequestParam(value = "role", required = false) String role, Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<User> users;

            if (role != null && !role.isEmpty()) {
                User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
                users = userService.getUsersByRole(userRole);
                model.addAttribute("selectedRole", userRole);
            } else {
                users = userService.getAllActiveUsers();
            }

            model.addAttribute("user", currentUser);
            model.addAttribute("users", users);
            model.addAttribute("roles", User.UserRole.values());

            return "admin/users";
        } catch (Exception e) {
            log.error("Error loading users", e);
            return "redirect:/admin/dashboard";
        }
    }

    @PostMapping("/users/{userId}/deactivate")
    public String deactivateUser(@PathVariable UUID userId, RedirectAttributes redirectAttributes) {
        try {
            userService.deactivateUser(userId);
            redirectAttributes.addFlashAttribute("message", "User deactivated successfully");
        } catch (Exception e) {
            log.error("Error deactivating user", e);
            redirectAttributes.addFlashAttribute("error", "Failed to deactivate user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{userId}/activate")
    public String activateUser(@PathVariable UUID userId, RedirectAttributes redirectAttributes) {
        try {
            userService.activateUser(userId);
            redirectAttributes.addFlashAttribute("message", "User activated successfully");
        } catch (Exception e) {
            log.error("Error activating user", e);
            redirectAttributes.addFlashAttribute("error", "Failed to activate user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/classes")
    public String classes(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Class> classes = classRepository.findAll();

            model.addAttribute("user", currentUser);
            model.addAttribute("classes", classes);

            return "admin/classes";
        } catch (Exception e) {
            log.error("Error loading classes", e);
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/rooms")
    public String rooms(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Room> rooms = roomRepository.findAll();

            model.addAttribute("user", currentUser);
            model.addAttribute("rooms", rooms);

            return "admin/rooms";
        } catch (Exception e) {
            log.error("Error loading rooms", e);
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/attendance")
    public String attendanceReports(@RequestParam(value = "date", required = false) LocalDate date, Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            if (date == null) {
                date = LocalDate.now();
            }

            List<AttendanceRecord> records = attendanceRepository.findByDate(date);

            // Calculate statistics
            long presentCount = records.stream()
                    .filter(record -> record.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT)
                    .count();
            long absentCount = records.stream()
                    .filter(record -> record.getStatus() == AttendanceRecord.AttendanceStatus.ABSENT)
                    .count();
            double attendanceRate = records.size() > 0 ? (double) presentCount / records.size() * 100 : 0;

            model.addAttribute("user", currentUser);
            model.addAttribute("records", records);
            model.addAttribute("selectedDate", date);
            model.addAttribute("presentCount", presentCount);
            model.addAttribute("absentCount", absentCount);
            model.addAttribute("totalCount", records.size());
            model.addAttribute("attendanceRate", attendanceRate);

            return "admin/attendance";
        } catch (Exception e) {
            log.error("Error loading attendance reports", e);
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/bookings")
    public String bookingReports(@RequestParam(value = "date", required = false) LocalDate date, Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            if (date == null) {
                date = LocalDate.now();
            }

            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            List<RoomBooking> bookings = roomBookingRepository.findBookingsInDateRange(startOfDay, endOfDay);

            // Calculate statistics
            long confirmedCount = bookings.stream()
                    .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.CONFIRMED)
                    .count();
            long cancelledCount = bookings.stream()
                    .filter(booking -> booking.getStatus() == RoomBooking.BookingStatus.CANCELLED)
                    .count();

            model.addAttribute("user", currentUser);
            model.addAttribute("bookings", bookings);
            model.addAttribute("selectedDate", date);
            model.addAttribute("confirmedCount", confirmedCount);
            model.addAttribute("cancelledCount", cancelledCount);
            model.addAttribute("totalCount", bookings.size());

            return "admin/bookings";
        } catch (Exception e) {
            log.error("Error loading booking reports", e);
            return "redirect:/admin/dashboard";
        }
    }

    @GetMapping("/system-info")
    public String systemInfo(Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            // System information
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            // Database statistics
            long totalUsers = userRepository.count();
            long totalClasses = classRepository.count();
            long totalRooms = roomRepository.count();
            long totalAttendance = attendanceRepository.count();
            long totalBookings = roomBookingRepository.count();

            model.addAttribute("user", currentUser);
            model.addAttribute("totalMemory", totalMemory);
            model.addAttribute("freeMemory", freeMemory);
            model.addAttribute("usedMemory", usedMemory);
            model.addAttribute("maxMemory", maxMemory);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalClasses", totalClasses);
            model.addAttribute("totalRooms", totalRooms);
            model.addAttribute("totalAttendance", totalAttendance);
            model.addAttribute("totalBookings", totalBookings);

            return "admin/system-info";
        } catch (Exception e) {
            log.error("Error loading system info", e);
            return "redirect:/admin/dashboard";
        }
    }
}