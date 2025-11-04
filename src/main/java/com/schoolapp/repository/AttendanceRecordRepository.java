package com.schoolapp.repository;

import com.schoolapp.model.AttendanceRecord;
import com.schoolapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findByStudent(User student);

    List<AttendanceRecord> findByStudentId(UUID studentId);

    List<AttendanceRecord> findByClassId(UUID classId);

    List<AttendanceRecord> findByTeacherId(UUID teacherId);

    List<AttendanceRecord> findByDate(LocalDate date);

    List<AttendanceRecord> findByStatus(AttendanceRecord.AttendanceStatus status);

    Optional<AttendanceRecord> findByStudentIdAndClassIdAndDate(UUID studentId, UUID classId, LocalDate date);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.student.id = :studentId ORDER BY ar.date DESC")
    List<AttendanceRecord> findByStudentIdOrderByDateDesc(@Param("studentId") UUID studentId);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.class.id = :classId AND ar.date = :date ORDER BY ar.student.firstName, ar.student.lastName")
    List<AttendanceRecord> findByClassIdAndDate(@Param("classId") UUID classId, @Param("date") LocalDate date);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.class.id = :classId AND ar.date BETWEEN :startDate AND :endDate ORDER BY ar.date DESC, ar.student.firstName, ar.student.lastName")
    List<AttendanceRecord> findByClassIdAndDateRange(@Param("classId") UUID classId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.date BETWEEN :startDate AND :endDate ORDER BY ar.date DESC")
    List<AttendanceRecord> findByStudentIdAndDateRange(@Param("studentId") UUID studentId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.status = :status")
    long countByStudentIdAndStatus(@Param("studentId") UUID studentId, @Param("status") AttendanceRecord.AttendanceStatus status);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.class.id = :classId AND ar.date = :date AND ar.status = :status")
    long countByClassIdAndDateAndStatus(@Param("classId") UUID classId, @Param("date") LocalDate date, @Param("status") AttendanceRecord.AttendanceStatus status);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.class.id = :classId AND ar.date = :date")
    long countByClassIdAndDate(@Param("classId") UUID classId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT ar.date FROM AttendanceRecord ar WHERE ar.class.id = :classId ORDER BY ar.date DESC")
    List<LocalDate> findDistinctDatesByClassId(@Param("classId") UUID classId);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.teacher.id = :teacherId AND ar.date = :date ORDER BY ar.class.name, ar.student.firstName, ar.student.lastName")
    List<AttendanceRecord> findByTeacherIdAndDate(@Param("teacherId") UUID teacherId, @Param("date") LocalDate date);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.class.id = :classId ORDER BY ar.date DESC")
    List<AttendanceRecord> findByStudentIdAndClassId(@Param("studentId") UUID studentId, @Param("classId") UUID classId);
}