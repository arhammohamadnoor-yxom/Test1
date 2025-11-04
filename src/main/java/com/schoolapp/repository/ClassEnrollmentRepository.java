package com.schoolapp.repository;

import com.schoolapp.model.ClassEnrollment;
import com.schoolapp.model.Class;
import com.schoolapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, UUID> {

    List<ClassEnrollment> findByClass(Class class_);

    List<ClassEnrollment> findByClassId(UUID classId);

    List<ClassEnrollment> findByStudent(User student);

    List<ClassEnrollment> findByStudentId(UUID studentId);

    List<ClassEnrollment> findByClassIdAndIsActive(UUID classId, Boolean isActive);

    List<ClassEnrollment> findByStudentIdAndIsActive(UUID studentId, Boolean isActive);

    Optional<ClassEnrollment> findByClassIdAndStudentId(UUID classId, UUID studentId);

    @Query("SELECT COUNT(e) FROM ClassEnrollment e WHERE e.class.id = :classId AND e.isActive = true")
    long countActiveEnrollmentsByClassId(@Param("classId") UUID classId);

    @Query("SELECT COUNT(e) FROM ClassEnrollment e WHERE e.student.id = :studentId AND e.isActive = true")
    long countActiveEnrollmentsByStudentId(@Param("studentId") UUID studentId);

    @Query("SELECT e FROM ClassEnrollment e WHERE e.class.id = :classId AND e.isActive = true ORDER BY e.student.firstName, e.student.lastName")
    List<ClassEnrollment> findActiveEnrollmentsByClassId(@Param("classId") UUID classId);

    @Query("SELECT e FROM ClassEnrollment e WHERE e.student.id = :studentId AND e.isActive = true ORDER BY e.class.name")
    List<ClassEnrollment> findActiveEnrollmentsByStudentId(@Param("studentId") UUID studentId);
}