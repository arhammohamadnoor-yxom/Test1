package com.schoolapp.controller;

import com.schoolapp.model.AttendanceRecord;
import com.schoolapp.model.Class;
import com.schoolapp.model.User;
import com.schoolapp.service.AttendanceService;
import com.schoolapp.service.AuthService;
import com.schoolapp.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
public class StudentController {

    private final AttendanceService attendanceService;
    private final AuthService authService;
    private final ClassRepository classRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            // Get student's enrolled classes
            List<Class> enrolledClasses = classRepository.findClassesByStudent(currentUser.getId());

            // Calculate attendance statistics for the current month
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate today = LocalDate.now();

            Map<UUID, Double> attendancePercentages = new HashMap<>();
            Map<UUID, Map<AttendanceRecord.AttendanceStatus, Long>> attendanceStats = new HashMap<>();

            for (Class classEntity : enrolledClasses) {
                try {
                    double percentage = attendanceService.getStudentAttendancePercentage(currentUser.getId(), startOfMonth, today);
                    attendancePercentages.put(classEntity.getId(), percentage);

                    Map<AttendanceRecord.AttendanceStatus, Long> stats = attendanceService.getStudentAttendanceStats(currentUser.getId(), startOfMonth, today);
                    attendanceStats.put(classEntity.getId(), stats);
                } catch (Exception e) {
                    // If no attendance records exist for this period
                    attendancePercentages.put(classEntity.getId(), 0.0);
                    attendanceStats.put(classEntity.getId(), new HashMap<>());
                }
            }

            // Calculate overall attendance percentage
            double overallPercentage = attendancePercentages.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            model.addAttribute("user", currentUser);
            model.addAttribute("enrolledClasses", enrolledClasses);
            model.addAttribute("attendancePercentages", attendancePercentages);
            model.addAttribute("attendanceStats", attendanceStats);
            model.addAttribute("overallPercentage", overallPercentage);
            model.addAttribute("totalClasses", enrolledClasses.size());

            return "student/dashboard";
        } catch (Exception e) {
            log.error("Error loading student dashboard", e);
            return "redirect:/login";
        }
    }

    @GetMapping("/attendance")
    public String attendancePage(@RequestParam(value = "classId", required = false) UUID classId,
                               @RequestParam(value = "month", required = false) Integer month,
                               @RequestParam(value = "year", required = false) Integer year,
                               Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            // Get student's enrolled classes
            List<Class> enrolledClasses = classRepository.findClassesByStudent(currentUser.getId());

            if (classId == null && !enrolledClasses.isEmpty()) {
                classId = enrolledClasses.get(0).getId();
            }

            // Default to current month if not specified
            LocalDate today = LocalDate.now();
            if (month == null) month = today.getMonthValue();
            if (year == null) year = today.getYear();

            LocalDate startDate = LocalDate.of(year, month, 1);
            LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

            List<AttendanceRecord> attendanceRecords = new ArrayList<>();
            Map<LocalDate, AttendanceRecord> attendanceByDate = new HashMap<>();

            if (classId != null) {
                attendanceRecords = attendanceService.getStudentAttendanceHistory(currentUser.getId(), startDate, endDate);
                attendanceByDate = attendanceRecords.stream()
                        .filter(record -> record.getClass_().getId().equals(classId))
                        .collect(Collectors.toMap(AttendanceRecord::getDate, record -> record));
            }

            Class selectedClass = null;
            if (classId != null) {
                selectedClass = classRepository.findById(classId)
                        .orElseThrow(() -> new RuntimeException("Class not found"));
            }

            // Calculate statistics for the selected period
            Map<AttendanceRecord.AttendanceStatus, Long> stats = new HashMap<>();
            double attendancePercentage = 0.0;

            if (classId != null) {
                stats = attendanceService.getStudentAttendanceStats(currentUser.getId(), startDate, endDate);
                attendancePercentage = attendanceService.getStudentAttendancePercentage(currentUser.getId(), startDate, endDate);
            }

            model.addAttribute("user", currentUser);
            model.addAttribute("enrolledClasses", enrolledClasses);
            model.addAttribute("selectedClass", selectedClass);
            model.addAttribute("selectedClassId", classId);
            model.addAttribute("selectedMonth", month);
            model.addAttribute("selectedYear", year);
            model.addAttribute("attendanceRecords", attendanceRecords);
            model.addAttribute("attendanceByDate", attendanceByDate);
            model.addAttribute("attendanceStats", stats);
            model.addAttribute("attendancePercentage", attendancePercentage);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            return "student/attendance";
        } catch (Exception e) {
            log.error("Error loading student attendance page", e);
            return "redirect:/student/dashboard";
        }
    }

    @GetMapping("/attendance/detail")
    public String attendanceDetail(@RequestParam UUID classId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  Model model) {
        try {
            User currentUser = authService.getCurrentUser();

            Class classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found"));

            List<AttendanceRecord> records = attendanceService.getStudentAttendanceHistory(currentUser.getId(), date, date);
            AttendanceRecord attendanceRecord = records.stream()
                    .filter(record -> record.getClass_().getId().equals(classId))
                    .findFirst()
                    .orElse(null);

            model.addAttribute("user", currentUser);
            model.addAttribute("classEntity", classEntity);
            model.addAttribute("attendanceRecord", attendanceRecord);
            model.addAttribute("date", date);

            return "student/attendance-detail";
        } catch (Exception e) {
            log.error("Error loading attendance detail", e);
            return "redirect:/student/attendance";
        }
    }

    @GetMapping("/my-classes")
    public String myClasses(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Class> enrolledClasses = classRepository.findClassesByStudent(currentUser.getId());

            // Calculate attendance summary for each class
            Map<UUID, Double> attendanceSummary = new HashMap<>();
            LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
            LocalDate today = LocalDate.now();

            for (Class classEntity : enrolledClasses) {
                try {
                    double percentage = attendanceService.getStudentAttendancePercentage(currentUser.getId(), startOfMonth, today);
                    attendanceSummary.put(classEntity.getId(), percentage);
                } catch (Exception e) {
                    attendanceSummary.put(classEntity.getId(), 0.0);
                }
            }

            model.addAttribute("user", currentUser);
            model.addAttribute("enrolledClasses", enrolledClasses);
            model.addAttribute("attendanceSummary", attendanceSummary);

            return "student/classes";
        } catch (Exception e) {
            log.error("Error loading student classes", e);
            return "redirect:/student/dashboard";
        }
    }

    @GetMapping("/rooms")
    public String rooms(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Class> enrolledClasses = classRepository.findClassesByStudent(currentUser.getId());

            model.addAttribute("user", currentUser);
            model.addAttribute("enrolledClasses", enrolledClasses);

            return "student/rooms";
        } catch (Exception e) {
            log.error("Error loading student rooms page", e);
            return "redirect:/student/dashboard";
        }
    }
}