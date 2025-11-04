package com.schoolapp.service;

import com.schoolapp.dto.AttendanceUpdateRequest;
import com.schoolapp.model.AttendanceRecord;
import com.schoolapp.model.Class;
import com.schoolapp.model.User;
import com.schoolapp.repository.AttendanceRecordRepository;
import com.schoolapp.repository.ClassRepository;
import com.schoolapp.repository.ClassEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRepository;
    private final ClassRepository classRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final AuthService authService;

    @Transactional
    public List<AttendanceRecord> markAttendanceForClass(UUID classId, LocalDate date, Map<UUID, AttendanceRecord.AttendanceStatus> attendanceData, Map<UUID, String> notes) {
        // Validate that the current user is a teacher and is assigned to this class
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != User.UserRole.TEACHER) {
            throw new RuntimeException("Only teachers can mark attendance");
        }

        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

        if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only mark attendance for your own classes");
        }

        // Get all active enrolled students
        List<User> enrolledStudents = enrollmentRepository.findActiveEnrollmentsByClassId(classId)
                .stream()
                .map(ClassEnrollment::getStudent)
                .collect(Collectors.toList());

        List<AttendanceRecord> records = new ArrayList<>();

        for (User student : enrolledStudents) {
            AttendanceRecord.AttendanceStatus status = attendanceData.getOrDefault(student.getId(), AttendanceRecord.AttendanceStatus.ABSENT);
            String studentNotes = notes.getOrDefault(student.getId(), null);

            // Check if attendance record already exists
            Optional<AttendanceRecord> existingRecord = attendanceRepository
                    .findByStudentIdAndClassIdAndDate(student.getId(), classId, date);

            AttendanceRecord record;
            if (existingRecord.isPresent()) {
                record = existingRecord.get();
                record.setStatus(status);
                record.setNotes(studentNotes);
            } else {
                record = AttendanceRecord.builder()
                        .student(student)
                        .class_(classEntity)
                        .teacher(currentUser)
                        .date(date)
                        .status(status)
                        .notes(studentNotes)
                        .build();
            }

            records.add(attendanceRepository.save(record));
        }

        return records;
    }

    @Transactional
    public List<AttendanceRecord> markAllPresent(UUID classId, LocalDate date) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != User.UserRole.TEACHER) {
            throw new RuntimeException("Only teachers can mark attendance");
        }

        Class classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

        if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only mark attendance for your own classes");
        }

        List<User> enrolledStudents = enrollmentRepository.findActiveEnrollmentsByClassId(classId)
                .stream()
                .map(ClassEnrollment::getStudent)
                .collect(Collectors.toList());

        List<AttendanceRecord> records = new ArrayList<>();

        for (User student : enrolledStudents) {
            Optional<AttendanceRecord> existingRecord = attendanceRepository
                    .findByStudentIdAndClassIdAndDate(student.getId(), classId, date);

            AttendanceRecord record;
            if (existingRecord.isPresent()) {
                record = existingRecord.get();
                record.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
            } else {
                record = AttendanceRecord.builder()
                        .student(student)
                        .class_(classEntity)
                        .teacher(currentUser)
                        .date(date)
                        .status(AttendanceRecord.AttendanceStatus.PRESENT)
                        .build();
            }

            records.add(attendanceRepository.save(record));
        }

        return records;
    }

    @Transactional
    public AttendanceRecord updateAttendance(UUID recordId, AttendanceUpdateRequest updateRequest) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != User.UserRole.TEACHER) {
            throw new RuntimeException("Only teachers can update attendance");
        }

        AttendanceRecord record = attendanceRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found: " + recordId));

        // Verify the teacher owns this class
        if (!record.getTeacher().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only update attendance for your own classes");
        }

        record.setStatus(updateRequest.getStatus());
        record.setNotes(updateRequest.getNotes());

        return attendanceRepository.save(record);
    }

    public List<AttendanceRecord> getAttendanceByClassAndDate(UUID classId, LocalDate date) {
        User currentUser = authService.getCurrentUser();

        // Students can only view their own attendance
        if (currentUser.getRole() == User.UserRole.STUDENT) {
            return attendanceRepository.findByStudentIdAndClassId(currentUser.getId(), classId)
                    .stream()
                    .filter(record -> record.getDate().equals(date))
                    .collect(Collectors.toList());
        }

        // Teachers can view their class attendance
        if (currentUser.getRole() == User.UserRole.TEACHER) {
            Class classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

            if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You can only view attendance for your own classes");
            }
        }

        return attendanceRepository.findByClassIdAndDate(classId, date);
    }

    public List<AttendanceRecord> getStudentAttendanceHistory(UUID studentId, LocalDate startDate, LocalDate endDate) {
        User currentUser = authService.getCurrentUser();

        // Students can only view their own attendance
        if (currentUser.getRole() == User.UserRole.STUDENT && !currentUser.getId().equals(studentId)) {
            throw new RuntimeException("You can only view your own attendance");
        }

        // Teachers can view attendance for their students
        if (currentUser.getRole() == User.UserRole.TEACHER) {
            List<Class> teacherClasses = classRepository.findByTeacherId(currentUser.getId());
            List<UUID> teacherClassIds = teacherClasses.stream().map(Class::getId).collect(Collectors.toList());

            return attendanceRepository.findByStudentIdAndDateRange(studentId, startDate, endDate)
                    .stream()
                    .filter(record -> teacherClassIds.contains(record.getClass_().getId()))
                    .collect(Collectors.toList());
        }

        // Admins can view any student's attendance
        return attendanceRepository.findByStudentIdAndDateRange(studentId, startDate, endDate);
    }

    public List<LocalDate> getAttendanceDatesForClass(UUID classId) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() == User.UserRole.TEACHER) {
            Class classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

            if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You can only view attendance dates for your own classes");
            }
        }

        return attendanceRepository.findDistinctDatesByClassId(classId);
    }

    public Map<AttendanceRecord.AttendanceStatus, Long> getAttendanceStats(UUID classId, LocalDate startDate, LocalDate endDate) {
        List<AttendanceRecord> records = attendanceRepository.findByClassIdAndDateRange(classId, startDate, endDate);

        return records.stream()
                .collect(Collectors.groupingBy(
                        AttendanceRecord::getStatus,
                        Collectors.counting()
                ));
    }

    public Map<AttendanceRecord.AttendanceStatus, Long> getStudentAttendanceStats(UUID studentId, LocalDate startDate, LocalDate endDate) {
        User currentUser = authService.getCurrentUser();

        // Students can only view their own stats
        if (currentUser.getRole() == User.UserRole.STUDENT && !currentUser.getId().equals(studentId)) {
            throw new RuntimeException("You can only view your own attendance statistics");
        }

        List<AttendanceRecord> records = attendanceRepository.findByStudentIdAndDateRange(studentId, startDate, endDate);

        return records.stream()
                .collect(Collectors.groupingBy(
                        AttendanceRecord::getStatus,
                        Collectors.counting()
                ));
    }

    public double getAttendancePercentage(UUID classId, LocalDate startDate, LocalDate endDate) {
        Map<AttendanceRecord.AttendanceStatus, Long> stats = getAttendanceStats(classId, startDate, endDate);
        long total = stats.values().stream().mapToLong(Long::longValue).sum();
        long present = stats.getOrDefault(AttendanceRecord.AttendanceStatus.PRESENT, 0L);

        return total > 0 ? (double) present / total * 100 : 0;
    }

    public double getStudentAttendancePercentage(UUID studentId, LocalDate startDate, LocalDate endDate) {
        Map<AttendanceRecord.AttendanceStatus, Long> stats = getStudentAttendanceStats(studentId, startDate, endDate);
        long total = stats.values().stream().mapToLong(Long::longValue).sum();
        long present = stats.getOrDefault(AttendanceRecord.AttendanceStatus.PRESENT, 0L);

        return total > 0 ? (double) present / total * 100 : 0;
    }

    public List<AttendanceRecord> getTodayAttendanceForTeacher(UUID teacherId) {
        return attendanceRepository.findByTeacherIdAndDate(teacherId, LocalDate.now());
    }

    public List<AttendanceRecord> getAttendanceForDateRange(UUID classId, LocalDate startDate, LocalDate endDate) {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getRole() == User.UserRole.TEACHER) {
            Class classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found: " + classId));

            if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You can only view attendance for your own classes");
            }
        }

        return attendanceRepository.findByClassIdAndDateRange(classId, startDate, endDate);
    }
}