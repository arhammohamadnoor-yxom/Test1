package com.schoolapp.controller;

import com.schoolapp.dto.AttendanceUpdateRequest;
import com.schoolapp.dto.RoomBookingRequest;
import com.schoolapp.model.AttendanceRecord;
import com.schoolapp.model.Class;
import com.schoolapp.model.Room;
import com.schoolapp.model.RoomBooking;
import com.schoolapp.model.User;
import com.schoolapp.service.AttendanceService;
import com.schoolapp.service.AuthService;
import com.schoolapp.service.RoomBookingService;
import com.schoolapp.repository.ClassRepository;
import com.schoolapp.repository.RoomRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {

    private final AttendanceService attendanceService;
    private final AuthService authService;
    private final RoomBookingService roomBookingService;
    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Class> teacherClasses = classRepository.findByTeacherId(currentUser.getId());

            // Get today's attendance summary
            List<AttendanceRecord> todayAttendance = attendanceService.getTodayAttendanceForTeacher(currentUser.getId());
            Map<UUID, Long> todayAttendanceByClass = new HashMap<>();
            Map<UUID, Long> presentCountByClass = new HashMap<>();

            for (AttendanceRecord record : todayAttendance) {
                UUID classId = record.getClass_().getId();
                todayAttendanceByClass.merge(classId, 1L, Long::sum);
                if (record.getStatus() == AttendanceRecord.AttendanceStatus.PRESENT) {
                    presentCountByClass.merge(classId, 1L, Long::sum);
                }
            }

            model.addAttribute("user", currentUser);
            model.addAttribute("classes", teacherClasses);
            model.addAttribute("todayAttendance", todayAttendanceByClass);
            model.addAttribute("presentCount", presentCountByClass);
            model.addAttribute("totalClasses", teacherClasses.size());
            model.addAttribute("todayRecords", todayAttendance.size());

            return "teacher/dashboard";
        } catch (Exception e) {
            log.error("Error loading teacher dashboard", e);
            return "redirect:/login";
        }
    }

    @GetMapping("/attendance")
    public String attendancePage(@RequestParam(value = "classId", required = false) UUID classId,
                               @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Class> teacherClasses = classRepository.findByTeacherId(currentUser.getId());

            if (classId == null && !teacherClasses.isEmpty()) {
                classId = teacherClasses.get(0).getId();
            }

            if (date == null) {
                date = LocalDate.now();
            }

            List<AttendanceRecord> attendanceRecords = new ArrayList<>();
            Map<UUID, AttendanceRecord> attendanceByStudent = new HashMap<>();

            if (classId != null) {
                Class selectedClass = classRepository.findById(classId)
                        .orElseThrow(() -> new RuntimeException("Class not found"));

                if (!selectedClass.getTeacher().getId().equals(currentUser.getId())) {
                    throw new RuntimeException("You can only view attendance for your own classes");
                }

                attendanceRecords = attendanceService.getAttendanceByClassAndDate(classId, date);
                attendanceByStudent = attendanceRecords.stream()
                        .collect(HashMap::new, (m, r) -> m.put(r.getStudent().getId(), r), HashMap::putAll);

                model.addAttribute("selectedClass", selectedClass);
                model.addAttribute("enrolledStudents", selectedClass.getEnrollments().stream()
                        .filter(enrollment -> enrollment.getIsActive())
                        .toList());
            }

            model.addAttribute("user", currentUser);
            model.addAttribute("classes", teacherClasses);
            model.addAttribute("selectedClassId", classId);
            model.addAttribute("selectedDate", date);
            model.addAttribute("attendanceRecords", attendanceRecords);
            model.addAttribute("attendanceByStudent", attendanceByStudent);

            return "teacher/attendance";
        } catch (Exception e) {
            log.error("Error loading attendance page", e);
            return "redirect:/teacher/dashboard";
        }
    }

    @PostMapping("/attendance/mark")
    public String markAttendance(@RequestParam UUID classId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestParam Map<String, String> formData,
                                RedirectAttributes redirectAttributes) {
        try {
            Map<UUID, AttendanceRecord.AttendanceStatus> attendanceData = new HashMap<>();
            Map<UUID, String> notes = new HashMap<>();

            for (Map.Entry<String, String> entry : formData.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("status_")) {
                    UUID studentId = UUID.fromString(key.substring(7));
                    AttendanceRecord.AttendanceStatus status = AttendanceRecord.AttendanceStatus.valueOf(value);
                    attendanceData.put(studentId, status);
                } else if (key.startsWith("notes_")) {
                    UUID studentId = UUID.fromString(key.substring(6));
                    notes.put(studentId, value.isEmpty() ? null : value);
                }
            }

            List<AttendanceRecord> records = attendanceService.markAttendanceForClass(classId, date, attendanceData, notes);

            redirectAttributes.addFlashAttribute("message", "Attendance marked successfully for " + records.size() + " students");
            return "redirect:/teacher/attendance?classId=" + classId + "&date=" + date;

        } catch (Exception e) {
            log.error("Error marking attendance", e);
            redirectAttributes.addFlashAttribute("error", "Failed to mark attendance: " + e.getMessage());
            return "redirect:/teacher/attendance?classId=" + classId + "&date=" + date;
        }
    }

    @PostMapping("/attendance/mark-all-present")
    public String markAllPresent(@RequestParam UUID classId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                RedirectAttributes redirectAttributes) {
        try {
            List<AttendanceRecord> records = attendanceService.markAllPresent(classId, date);
            redirectAttributes.addFlashAttribute("message", "All " + records.size() + " students marked as present");
            return "redirect:/teacher/attendance?classId=" + classId + "&date=" + date;

        } catch (Exception e) {
            log.error("Error marking all present", e);
            redirectAttributes.addFlashAttribute("error", "Failed to mark all present: " + e.getMessage());
            return "redirect:/teacher/attendance?classId=" + classId + "&date=" + date;
        }
    }

    @PostMapping("/attendance/update")
    public String updateAttendance(@RequestParam UUID recordId,
                                  @Valid @ModelAttribute AttendanceUpdateRequest updateRequest,
                                  RedirectAttributes redirectAttributes) {
        try {
            AttendanceRecord record = attendanceService.updateAttendance(recordId, updateRequest);
            redirectAttributes.addFlashAttribute("message", "Attendance updated successfully");
            return "redirect:/teacher/attendance?classId=" + record.getClass_().getId() + "&date=" + record.getDate();

        } catch (Exception e) {
            log.error("Error updating attendance", e);
            redirectAttributes.addFlashAttribute("error", "Failed to update attendance: " + e.getMessage());
            return "redirect:/teacher/attendance";
        }
    }

    @GetMapping("/attendance/history")
    public String attendanceHistory(@RequestParam UUID classId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                   Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            Class classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found"));

            if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You can only view attendance for your own classes");
            }

            List<AttendanceRecord> records = attendanceService.getAttendanceForDateRange(classId, startDate, endDate);
            Map<LocalDate, Map<AttendanceRecord.AttendanceStatus, Long>> dailyStats = new HashMap<>();

            for (AttendanceRecord record : records) {
                LocalDate date = record.getDate();
                dailyStats.computeIfAbsent(date, k -> new HashMap<>())
                        .merge(record.getStatus(), 1L, Long::sum);
            }

            double overallPercentage = attendanceService.getAttendancePercentage(classId, startDate, endDate);

            model.addAttribute("user", currentUser);
            model.addAttribute("classEntity", classEntity);
            model.addAttribute("records", records);
            model.addAttribute("dailyStats", dailyStats);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("overallPercentage", overallPercentage);

            return "teacher/attendance-history";
        } catch (Exception e) {
            log.error("Error loading attendance history", e);
            return "redirect:/teacher/attendance";
        }
    }

    @GetMapping("/my-classes")
    public String myClasses(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Class> classes = classRepository.findByTeacherId(currentUser.getId());

            model.addAttribute("user", currentUser);
            model.addAttribute("classes", classes);

            return "teacher/classes";
        } catch (Exception e) {
            log.error("Error loading teacher classes", e);
            return "redirect:/teacher/dashboard";
        }
    }

    // Room Booking Methods
    @GetMapping("/booking")
    public String bookingPage(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            List<Class> teacherClasses = classRepository.findByTeacherId(currentUser.getId());
            List<Room> rooms = roomRepository.findActiveRooms();
            List<RoomBooking> upcomingBookings = roomBookingService.getUpcomingBookingsForTeacher(currentUser.getId());

            model.addAttribute("user", currentUser);
            model.addAttribute("classes", teacherClasses);
            model.addAttribute("rooms", rooms);
            model.addAttribute("upcomingBookings", upcomingBookings);

            return "teacher/booking";
        } catch (Exception e) {
            log.error("Error loading booking page", e);
            return "redirect:/teacher/dashboard";
        }
    }

    @PostMapping("/booking/create")
    public String createBooking(@Valid @ModelAttribute RoomBookingRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            RoomBooking booking = roomBookingService.createBooking(request);
            redirectAttributes.addFlashAttribute("message", "Room booked successfully!");
            return "redirect:/teacher/booking";

        } catch (Exception e) {
            log.error("Error creating booking", e);
            redirectAttributes.addFlashAttribute("error", "Failed to book room: " + e.getMessage());
            return "redirect:/teacher/booking";
        }
    }

    @PostMapping("/booking/cancel")
    public String cancelBooking(@RequestParam UUID bookingId,
                               RedirectAttributes redirectAttributes) {
        try {
            roomBookingService.cancelBooking(bookingId);
            redirectAttributes.addFlashAttribute("message", "Booking cancelled successfully");
            return "redirect:/teacher/booking";

        } catch (Exception e) {
            log.error("Error cancelling booking", e);
            redirectAttributes.addFlashAttribute("error", "Failed to cancel booking: " + e.getMessage());
            return "redirect:/teacher/booking";
        }
    }

    @GetMapping("/booking/my-bookings")
    public String myBookings(Model model) {
        try {
            User currentUser = authService.getCurrentUser();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneMonthLater = now.plusMonths(1);

            List<RoomBooking> bookings = roomBookingService.getBookingsForTeacher(
                    currentUser.getId(), now, oneMonthLater);

            model.addAttribute("user", currentUser);
            model.addAttribute("bookings", bookings);

            return "teacher/my-bookings";
        } catch (Exception e) {
            log.error("Error loading my bookings", e);
            return "redirect:/teacher/booking";
        }
    }
}